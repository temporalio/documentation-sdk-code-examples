package io.temporal.docs.bestpractices.errorhandling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityOptions;
import io.temporal.api.enums.v1.EventType;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.failure.ChildWorkflowFailure;
import io.temporal.testing.TestEnvironmentOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkflowImplementationOptions;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class ErrorHandlingTest {

    private static final String TASK_QUEUE = "error-handling";

    private TestWorkflowEnvironment testEnv;

    @AfterEach
    public void tearDown() {
        if (testEnv != null) {
            testEnv.close();
        }
    }

    private Worker newEnv() {
        testEnv = TestWorkflowEnvironment.newInstance();
        return testEnv.newWorker(TASK_QUEUE);
    }

    private OrderWorkflow stub(String workflowId) {
        return testEnv.getWorkflowClient().newWorkflowStub(OrderWorkflow.class,
            WorkflowOptions.newBuilder().setWorkflowId(workflowId).setTaskQueue(TASK_QUEUE).build());
    }

    private WorkflowExecutionStatus status(String workflowId) {
        return testEnv.getWorkflowClient().newUntypedWorkflowStub(workflowId).describe()
            .getWorkflowExecutionInfo().getStatus();
    }

    // ── wrap() ──────────────────────────────────────────────────────────────────────────────────

    @Test
    public void wrapOnlyChangesCheckedExceptions() {
        IOException checked = new IOException("io");
        RuntimeException wrapped = Activity.wrap(checked);
        assertSame(checked, wrapped.getCause());

        IllegalStateException unchecked = new IllegalStateException("unchecked");
        assertSame(unchecked, Activity.wrap(unchecked));

        AssertionError error = new AssertionError("error");
        assertSame(error, assertThrows(AssertionError.class, () -> Activity.wrap(error)));
    }

    @WorkflowInterface
    public interface GreetingWorkflow {
        @WorkflowMethod
        String greet(String name);
    }

    public static class GreetingWorkflowImpl implements GreetingWorkflow {
        private final GreetingActivities activities = Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(10))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
                .build());

        @Override
        public String greet(String name) {
            return activities.composeGreeting("Hello", name);
        }
    }

    @Test
    public void wrappedCheckedExceptionKeepsItsTypeAndMessage() {
        Worker worker = newEnv();
        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
        worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
        testEnv.start();

        GreetingWorkflow workflow = testEnv.getWorkflowClient().newWorkflowStub(GreetingWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
        assertEquals("Hello, World!", workflow.greet("World"));

        WorkflowFailedException e = assertThrows(WorkflowFailedException.class, () ->
            testEnv.getWorkflowClient().newWorkflowStub(GreetingWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build()).greet(""));
        ApplicationFailure failure = assertInstanceOf(ApplicationFailure.class,
            assertInstanceOf(ActivityFailure.class, e.getCause()).getCause());
        // type defaults to the fully qualified class name of the unwrapped exception.
        assertEquals("java.io.IOException", failure.getType());
        assertEquals("Greeting service unavailable", failure.getOriginalMessage());
    }

    // ── catching ────────────────────────────────────────────────────────────────────────────────

    @Test
    public void swallowedCancellationStillEndsCanceledAndDropsTheResult() {
        Worker worker = newEnv();
        worker.registerWorkflowImplementationTypes(SwallowingWorkflowImpl.class);
        worker.registerActivitiesImplementations(new PaymentActivitiesImpl());
        testEnv.start();

        OrderWorkflow workflow = stub("swallow");
        WorkflowClient.start(workflow::processOrder, new Order("order-1", BigDecimal.TEN));
        testEnv.sleep(Duration.ofMinutes(1));
        WorkflowStub.fromTyped(workflow).cancel();

        // The Workflow catches CanceledFailure and returns defaultResult(), but once cancellation
        // is requested the Java SDK closes the execution as Canceled, so the result is lost.
        assertThrows(WorkflowFailedException.class,
            () -> WorkflowStub.fromTyped(workflow).getResult(Result.class));
        assertEquals(WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_CANCELED, status("swallow"));
    }

    @Test
    public void narrowCatchHandlesOnlyValidationErrors() {
        Worker worker = newEnv();
        worker.registerWorkflowImplementationTypes(NarrowCatchWorkflowImpl.class);
        worker.registerActivitiesImplementations(new PaymentActivitiesImpl());
        testEnv.start();

        assertEquals("approved",
            stub("narrow-ok").processOrder(new Order("order-1", BigDecimal.TEN)).getStatus());
        assertEquals("rejected",
            stub("narrow-bad").processOrder(new Order("bad-card-1", BigDecimal.TEN)).getStatus());
    }

    @Test
    public void causeChainExposesTheOriginalMessage() {
        Worker worker = newEnv();
        worker.registerWorkflowImplementationTypes(CauseChainWorkflowImpl.class);
        worker.registerActivitiesImplementations(new PaymentActivitiesImpl());
        testEnv.start();

        Result result = stub("chain").processOrder(new Order("bad-card-1", BigDecimal.TEN));
        assertEquals("rejected", result.getStatus());
        assertEquals("Invalid credit card number", result.getMessage());

        ApplicationFailure failure =
            ApplicationFailure.newNonRetryableFailure("Invalid credit card number", "ValidationError");
        assertEquals(
            "message='Invalid credit card number', type='ValidationError', nonRetryable=true",
            failure.getMessage());
    }

    @WorkflowInterface
    public interface ParentWorkflow {
        @WorkflowMethod
        String run();
    }

    public static class ParentWorkflowImpl implements ParentWorkflow {
        @Override
        public String run() {
            return Workflow.newChildWorkflowStub(ChildWorkflow.class).run();
        }
    }

    @WorkflowInterface
    public interface ChildWorkflow {
        @WorkflowMethod
        String run();
    }

    public static class ChildWorkflowImpl implements ChildWorkflow {
        @Override
        public String run() {
            return Workflow.newActivityStub(PaymentActivities.class,
                ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
                    .build())
                .processCreditCard("bad-card-1");
        }
    }

    @Test
    public void failureFromAChildWorkflowsActivityHasTheDocumentedChain() {
        Worker worker = newEnv();
        worker.registerWorkflowImplementationTypes(ParentWorkflowImpl.class, ChildWorkflowImpl.class);
        worker.registerActivitiesImplementations(new PaymentActivitiesImpl());
        testEnv.start();

        WorkflowFailedException e = assertThrows(WorkflowFailedException.class, () ->
            testEnv.getWorkflowClient().newWorkflowStub(ParentWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build()).run());
        ChildWorkflowFailure child = assertInstanceOf(ChildWorkflowFailure.class, e.getCause());
        ActivityFailure activity = assertInstanceOf(ActivityFailure.class, child.getCause());
        ApplicationFailure app = assertInstanceOf(ApplicationFailure.class, activity.getCause());
        assertEquals("ValidationError", app.getType());
    }

    @Test
    public void interceptorTurnsDomainExceptionIntoTypedFailure() {
        testEnv = TestWorkflowEnvironment.newInstance(TestEnvironmentOptions.newBuilder()
            .setWorkerFactoryOptions(WorkerSetup.factoryOptions())
            .build());
        Worker worker = testEnv.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(ChargeWorkflowImpl.class);
        worker.registerActivitiesImplementations(new PaymentActivitiesImpl());
        testEnv.start();

        assertEquals("approved",
            stub("charge-ok").processOrder(new Order("order-1", BigDecimal.TEN)).getStatus());
        Result declined = stub("charge-declined").processOrder(new Order("declined-1", BigDecimal.TEN));
        assertEquals("declined", declined.getStatus());
        assertEquals("Insufficient funds", declined.getMessage());
    }

    // ── cancellation ────────────────────────────────────────────────────────────────────────────

    public static class CancellableActivities extends PaymentActivitiesImpl {
        static final AtomicBoolean compensated = new AtomicBoolean();

        @Override
        public void longRunningWork() {
            for (int i = 0; i < 100; i++) {
                Activity.getExecutionContext().heartbeat(i);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        @Override
        public void compensate() {
            compensated.set(true);
        }
    }

    @Test
    public void cancellationRunsCompensationAndEndsCanceled() {
        CancellableActivities.compensated.set(false);
        Worker worker = newEnv();
        worker.registerWorkflowImplementationTypes(CancellationWorkflowImpl.class);
        worker.registerActivitiesImplementations(new CancellableActivities());
        testEnv.start();

        OrderWorkflow workflow = stub("cancel");
        WorkflowClient.start(workflow::processOrder, new Order("order-1", BigDecimal.TEN));
        testEnv.sleep(Duration.ofSeconds(1));
        WorkflowStub.fromTyped(workflow).cancel();
        assertThrows(WorkflowFailedException.class,
            () -> WorkflowStub.fromTyped(workflow).getResult(Result.class));

        assertEquals(WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_CANCELED, status("cancel"));
        assertTrue(CancellableActivities.compensated.get(), "compensate() should have run");
    }

    // ── failing a Workflow ──────────────────────────────────────────────────────────────────────

    @Test
    public void applicationFailureFailsTheWorkflowExecution() {
        Worker worker = newEnv();
        worker.registerWorkflowImplementationTypes(OrderValidationWorkflowImpl.class);
        testEnv.start();

        WorkflowFailedException e = assertThrows(WorkflowFailedException.class,
            () -> stub("invalid").processOrder(new Order("order-1", BigDecimal.ZERO)));
        ApplicationFailure failure = assertInstanceOf(ApplicationFailure.class, e.getCause());
        assertEquals("InvalidOrderTotal", failure.getType());
    }

    @Test
    public void plainExceptionFailsOnlyTheWorkflowTask() {
        Worker worker = newEnv();
        worker.registerWorkflowImplementationTypes(BuggyWorkflowImpl.class);
        testEnv.start();

        OrderWorkflow workflow = stub("buggy");
        WorkflowClient.start(workflow::processOrder, new Order("order-1", BigDecimal.TEN));
        testEnv.sleep(Duration.ofSeconds(5));

        assertEquals(WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING, status("buggy"));
        assertTrue(testEnv.getWorkflowClient().fetchHistory("buggy").getEvents().stream()
            .anyMatch(ev -> ev.getEventType() == EventType.EVENT_TYPE_WORKFLOW_TASK_FAILED));
    }

    @Test
    public void failWorkflowExceptionTypesFailTheExecution() {
        Worker worker = newEnv();
        worker.registerWorkflowImplementationTypes(
            WorkflowImplementationOptions.newBuilder()
                .setFailWorkflowExceptionTypes(IllegalStateException.class)
                .build(),
            BuggyWorkflowImpl.class);
        testEnv.start();

        assertThrows(WorkflowFailedException.class,
            () -> stub("buggy-fail").processOrder(new Order("order-1", BigDecimal.TEN)));
        assertEquals(WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_FAILED, status("buggy-fail"));
    }
}
