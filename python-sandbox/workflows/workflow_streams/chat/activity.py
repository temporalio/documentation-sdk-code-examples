# @@@SNIPSTART python-workflow-streams-chat-activity
from dataclasses import dataclass
from datetime import timedelta

from openai import AsyncOpenAI
from temporalio import activity
from temporalio.contrib.workflow_streams import WorkflowStreamClient


@dataclass
class TextDelta:
    text: str


@activity.defn
async def stream_completion(prompt: str) -> str:
    stream_client = WorkflowStreamClient.from_within_activity(
        batch_interval=timedelta(milliseconds=200),
    )
    # Disable provider-side retries; let Temporal own retry policy at the Activity layer.
    openai_client = AsyncOpenAI(max_retries=0)

    async with stream_client:
        deltas = stream_client.topic("delta", type=TextDelta)
        retry = stream_client.topic("retry", type=dict)
        close = stream_client.topic("close")

        # Tell consumers an earlier attempt's deltas are stale.
        if activity.info().attempt > 1:
            retry.publish({"attempt": activity.info().attempt}, force_flush=True)

        full: list[str] = []
        first = True
        oai_stream = await openai_client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[{"role": "user", "content": prompt}],
            stream=True,
        )
        async for chunk in oai_stream:
            if not chunk.choices:
                continue
            text = chunk.choices[0].delta.content
            if not text:
                continue
            # force_flush only on the first delta so the user sees something
            # immediately; subsequent deltas batch at the 200 ms interval.
            deltas.publish(TextDelta(text=text), force_flush=first)
            first = False
            full.append(text)
        close.publish({})
    return "".join(full)
# @@@SNIPEND
