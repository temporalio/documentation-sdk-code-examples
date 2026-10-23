import asyncio

from temporalio import workflow
from temporalio.common import SearchAttributeKey

keyword_key = SearchAttributeKey.for_keyword_list("CustomKeywordField")
customer_id_key = SearchAttributeKey.for_keyword("CustomerId")


@workflow.defn
class GreetingWorkflow:
    @workflow.run
    async def run(self) -> None:
        # Replace the starting value, then remove it, so a Client can watch the changes.
        await asyncio.sleep(2)
        workflow.upsert_search_attributes([keyword_key.value_set(["new-value"])])
        await asyncio.sleep(2)
        workflow.upsert_search_attributes([keyword_key.value_unset()])


@workflow.defn
class CustomerWorkflow:
    @workflow.run
    async def run(self) -> None:
        # @@@SNIPSTART python-observability-upsert-search-attributes
        workflow.upsert_search_attributes([
            customer_id_key.value_set("customer_2")
        ])
        # @@@SNIPEND
        await workflow.wait_condition(lambda: self._remove)
        # @@@SNIPSTART python-observability-remove-search-attribute
        workflow.upsert_search_attributes([
            customer_id_key.value_unset()
        ])
        # @@@SNIPEND

    def __init__(self) -> None:
        self._remove = False

    @workflow.signal
    def remove_customer_id(self) -> None:
        self._remove = True
