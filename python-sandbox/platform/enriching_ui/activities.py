from temporalio import activity


@activity.defn
async def your_activity(input: str) -> str:
    return f"Processed {input}"
