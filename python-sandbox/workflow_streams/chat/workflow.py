# @@@SNIPSTART python-workflow-streams-chat-workflow
from dataclasses import dataclass
from datetime import timedelta

from temporalio import workflow
from temporalio.contrib.workflow_streams import WorkflowStream

with workflow.unsafe.imports_passed_through():
    from workflow_streams.chat.activity import stream_completion


@dataclass
class ChatInput:
    prompt: str


@workflow.defn
class ChatWorkflow:
    @workflow.init
    def __init__(self, input: ChatInput) -> None:
        self.stream = WorkflowStream()
        self.subscriber_done: bool = False

    @workflow.signal
    async def subscriber_acknowledged_terminator(self) -> None:
        self.subscriber_done = True

    @workflow.run
    async def run(self, input: ChatInput) -> str:
        result = await workflow.execute_activity(
            stream_completion,
            input.prompt,
            start_to_close_timeout=timedelta(minutes=5),
        )
        # Wait for the subscriber to ack the terminal `close` event.
        # The timeout is a fallback for when no subscriber is attached;
        # with the ack, the typical case exits as soon as the subscriber confirms.
        try:
            await workflow.wait_condition(
                lambda: self.subscriber_done,
                timeout=timedelta(seconds=30),
            )
        except TimeoutError:
            pass  # No subscriber; the run still completes cleanly.
        return result
# @@@SNIPEND
