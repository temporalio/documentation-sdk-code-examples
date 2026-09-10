import asyncio
from datetime import timedelta

from temporalio import workflow
from temporalio.contrib.strands import TemporalAgent
from temporalio.contrib.strands.workflow import activity_as_tool

with workflow.unsafe.imports_passed_through():
    from activities import execute_code

TASK_QUEUE = "durable-agent"
DEPLOYMENT_NAME = "durable-agent-agentcore"
BUILD_ID = "1.0.0"

SYSTEM_PROMPT = """You are a data-analysis assistant.
Use execute_code for calculations and report the code and its output.
Use earlier messages when answering follow-up questions."""


# @@@SNIPSTART python-agentcore-durable-agent-workflow
@workflow.defn
class DurableAgentWorkflow:
    def __init__(self) -> None:
        self._done = False
        self._lock = asyncio.Lock()
        self._agent = TemporalAgent(
            model="bedrock",
            start_to_close_timeout=timedelta(seconds=60),
            system_prompt=SYSTEM_PROMPT,
            tools=[
                activity_as_tool(
                    execute_code,
                    start_to_close_timeout=timedelta(minutes=2),
                )
            ],
        )

    @workflow.update
    async def ask(self, prompt: str) -> str:
        async with self._lock:
            result = await self._agent.invoke_async(prompt)
            return str(result).strip()

    @workflow.signal
    def finish(self) -> None:
        self._done = True

    @workflow.run
    async def run(self) -> None:
        await workflow.wait_condition(lambda: self._done)
        await workflow.wait_condition(workflow.all_handlers_finished)


# @@@SNIPEND
