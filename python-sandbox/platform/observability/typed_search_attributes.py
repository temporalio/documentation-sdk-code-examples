from temporalio.client import Client, WorkflowHandle
from temporalio.common import SearchAttributeKey, SearchAttributePair, TypedSearchAttributes

from observability.search_attributes_workflow import CustomerWorkflow


async def start_with_search_attributes(client: Client) -> WorkflowHandle:
    # @@@SNIPSTART python-observability-typed-search-attributes
    customer_id_key = SearchAttributeKey.for_keyword("CustomerId")
    misc_data_key = SearchAttributeKey.for_text("MiscData")

    handle = await client.start_workflow(
        CustomerWorkflow.run,
        id="search-attributes-workflow-id",
        task_queue="search-attributes-task-queue",
        search_attributes=TypedSearchAttributes([
            SearchAttributePair(customer_id_key, "customer_1"),
            SearchAttributePair(misc_data_key, "customer_1_data")
        ]),
    )
    # @@@SNIPEND
    return handle
