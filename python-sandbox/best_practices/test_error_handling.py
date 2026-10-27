import asyncio
import uuid

import pytest
import pytest_asyncio
from temporalio.client import WorkflowFailureError
from temporalio.exceptions import ApplicationError
from temporalio.testing import ActivityEnvironment, WorkflowEnvironment
from temporalio.worker import Worker

from error_handling import (
    activity_errors,
    checkout_workflow,
    idempotency,
    money_transfer,
    non_retryable,
    pizza_workflow,
    retry_policies,
    saga_workflow,
)


@pytest_asyncio.fixture
async def env():
    async with await WorkflowEnvironment.start_local() as env:
        yield env


async def run(env, workflow_run, arg, workflows, activities=()):
    task_queue = str(uuid.uuid4())
    async with Worker(
        env.client, task_queue=task_queue, workflows=workflows, activities=list(activities)
    ):
        handle = await env.client.start_workflow(
            workflow_run, arg, id=str(uuid.uuid4()), task_queue=task_queue
        )
        return handle, await handle.result()


@pytest.mark.asyncio
async def test_idempotency_key_dedupes_charges():
    activity_env = ActivityEnvironment()
    first = await activity_env.run(idempotency.process_payment, 10.0, "acct-1")
    # A retry has the same Run ID and Activity ID, so it reuses the original charge.
    second = await activity_env.run(idempotency.process_payment, 99.0, "acct-1")
    assert first == second == "charged 10.0 to acct-1"


@pytest.mark.asyncio
@pytest.mark.parametrize(
    "card, amount, error_type",
    [("1234", 10.0, "InvalidCreditCard"), ("1234567812345678", 0, "InvalidAmount")],
)
async def test_validate_charge_raises_application_errors(card, amount, error_type):
    with pytest.raises(ApplicationError) as err:
        await ActivityEnvironment().run(activity_errors.validate_charge, card, amount)
    assert err.value.type == error_type
    assert not err.value.non_retryable


@pytest.mark.asyncio
async def test_non_retryable_errors():
    with pytest.raises(ApplicationError) as err:
        await ActivityEnvironment().run(non_retryable.process_payment, "bad", 10.0)
    assert err.value.type == "InvalidCardFormat"
    assert err.value.non_retryable
    assert await ActivityEnvironment().run(
        non_retryable.process_payment, "1234567812345678", 10.0
    ) == "charged 10.0"


@pytest.mark.asyncio
async def test_application_error_fails_workflow(env):
    workflows = [pizza_workflow.PizzaDeliveryWorkflow, pizza_workflow.DeliveryRadiusWorkflow]
    activities = [pizza_workflow.calculate_distance]
    _, result = await run(
        env,
        pizza_workflow.PizzaDeliveryWorkflow.run,
        pizza_workflow.PizzaOrder(address="near", is_delivery=True),
        workflows,
        activities,
    )
    assert result == "Order accepted"

    for workflow_run, arg, error_type in [
        (pizza_workflow.PizzaDeliveryWorkflow.run, pizza_workflow.PizzaOrder("far away", True), "CustomerOutsideServiceArea"),
        (pizza_workflow.DeliveryRadiusWorkflow.run, "far away", "OutsideServiceArea"),
    ]:
        with pytest.raises(WorkflowFailureError) as err:
            await run(env, workflow_run, arg, workflows, activities)
        assert err.value.cause.type == error_type


@pytest.mark.asyncio
async def test_python_exception_fails_workflow_task(env):
    task_queue = str(uuid.uuid4())
    async with Worker(env.client, task_queue=task_queue, workflows=[pizza_workflow.BuggyWorkflow]):
        handle = await env.client.start_workflow(
            pizza_workflow.BuggyWorkflow.run, id=str(uuid.uuid4()), task_queue=task_queue
        )
        # The Workflow Task fails and retries, but the Workflow Execution stays open.
        for _ in range(50):
            history = await handle.fetch_history()
            if any(e.HasField("workflow_task_failed_event_attributes") for e in history.events):
                break
            await asyncio.sleep(0.2)
        assert any(e.HasField("workflow_task_failed_event_attributes") for e in history.events)
        assert (await handle.describe()).status.name == "RUNNING"
        await handle.terminate()


@pytest.mark.asyncio
async def test_money_transfer_refunds_when_deposit_fails(env):
    workflows = [money_transfer.MoneyTransferWorkflow]
    activities = [money_transfer.withdraw, money_transfer.deposit, money_transfer.refund]
    _, result = await run(
        env,
        money_transfer.MoneyTransferWorkflow.run,
        money_transfer.TransferDetails(10, "acct-a", "acct-b"),
        workflows,
        activities,
    )
    assert result.startswith("Transfer complete")

    # A deposit that keeps failing exhausts its Retry Policy, so the Workflow refunds.
    with pytest.raises(WorkflowFailureError) as err:
        await run(
            env,
            money_transfer.MoneyTransferWorkflow.run,
            money_transfer.TransferDetails(10, "acct-a", "closed-account"),
            workflows,
            activities,
        )
    assert err.value.cause.type == "DepositError"


@pytest.mark.asyncio
async def test_retry_policies(env):
    order = retry_policies.Order(order_id="order-1")
    _, result = await run(
        env,
        retry_policies.OrderWorkflow.run,
        order,
        [retry_policies.OrderWorkflow],
        [retry_policies.call_external_service],
    )
    assert result == "processed order-1"
    handle, _ = await run(
        env,
        retry_policies.TwoPoliciesWorkflow.run,
        order,
        [retry_policies.TwoPoliciesWorkflow],
        [retry_policies.process_order],
    )
    history = await handle.fetch_history()
    intervals = [
        e.activity_task_scheduled_event_attributes.retry_policy.initial_interval.seconds
        for e in history.events
        if e.HasField("activity_task_scheduled_event_attributes")
    ]
    assert intervals == [1, 30]


@pytest.mark.asyncio
async def test_non_retryable_error_types_stop_retries(env):
    handle, result = await run(
        env,
        checkout_workflow.CheckoutWorkflow.run,
        checkout_workflow.PaymentDetails(card_number="1234567812345678", amount=5000),
        [checkout_workflow.CheckoutWorkflow],
        [checkout_workflow.process_payment],
    )
    assert result.startswith("Payment declined")
    failed = next(
        e.activity_task_failed_event_attributes
        for e in (await handle.fetch_history()).events
        if e.HasField("activity_task_failed_event_attributes")
    )
    assert failed.failure.application_failure_info.type == "InsufficientFunds"


@pytest.mark.asyncio
async def test_saga_compensates_in_reverse_order(env):
    activities = [
        saga_workflow.reserve_inventory,
        saga_workflow.revert_inventory,
        saga_workflow.charge_payment,
        saga_workflow.refund_payment,
        saga_workflow.create_shipment,
        saga_workflow.cancel_shipment,
    ]
    saga_workflow.calls.clear()
    with pytest.raises(WorkflowFailureError) as err:
        await run(
            env,
            saga_workflow.OrderWorkflow.run,
            saga_workflow.Order(order_id="order-1", fail_shipment=True),
            [saga_workflow.OrderWorkflow],
            activities,
        )
    assert err.value.cause.type == "OrderFailed"
    assert saga_workflow.calls == [
        "reserve_inventory",
        "charge_payment",
        "create_shipment",
        "cancel_shipment",
        "refund_payment",
        "revert_inventory",
    ]
