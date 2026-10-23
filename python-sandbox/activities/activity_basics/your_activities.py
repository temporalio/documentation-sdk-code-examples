# @@@SNIPSTART python-activity-basics-activity
from temporalio import activity
from activity_basics.your_dataobject import YourParams


@activity.defn(name="your_activity")
async def your_activity(input: YourParams) -> str:
    return f"{input.greeting}, {input.name}!"
# @@@SNIPEND
