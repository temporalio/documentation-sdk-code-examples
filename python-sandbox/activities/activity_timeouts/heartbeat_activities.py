from temporalio import activity


# @@@SNIPSTART python-activity-timeouts-heartbeat
@activity.defn
async def your_activity_definition() -> str:
    activity.heartbeat("heartbeat details!")
    # @@@SNIPEND
    return "done"


@activity.defn(name="your-activity")
async def your_heartbeating_activity(name: str = "World") -> str:
    activity.heartbeat(f"working on {name}")
    return f"Hello, {name}!"
