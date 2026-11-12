from temporalio import activity

from shared import SingleRecord

# The dataset is always 3 pages long.
PAGE_COUNT = 3


@activity.defn
async def get_records(page_size: int, offset: int) -> list[SingleRecord]:
    """Returns the next page of records, or an empty list when there are none left.

    Records are generated in memory so the sample needs no external storage. A
    real application would page through a database table, a file, or an object
    store here.
    """
    if offset >= page_size * PAGE_COUNT:
        return []
    return [SingleRecord(id=offset + i) for i in range(page_size)]
