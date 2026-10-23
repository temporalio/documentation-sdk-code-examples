# @@@SNIPSTART python-agentcore-heartbeat-activity
from temporalio import activity


@activity.defn
async def my_activity(items: list[str]) -> str:
    # Resume after the last item a previous attempt recorded, if there was one.
    details = activity.info().heartbeat_details
    start = details[0] + 1 if details else 0
    for i, item in enumerate(items[start:], start=start):
        activity.heartbeat(i)
        # ... process item
    return "done"
# @@@SNIPEND
