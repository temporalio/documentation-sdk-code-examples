from temporalio import activity


@activity.defn
async def my_activity(name: str) -> str:
    return f"Hello, {name}!"
