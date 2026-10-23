# @@@SNIPSTART python-message-passing-async-handler
import asyncio
from datetime import timedelta
from typing import Optional

from temporalio import activity, workflow
from temporalio.exceptions import ApplicationError

with workflow.unsafe.imports_passed_through():
    from message_passing.greeting_workflow import Language


@activity.defn
async def call_greeting_service(to_language: Language) -> Optional[str]:
    await asyncio.sleep(0.2)  # Pretend that we are calling a remote service.
    greetings = {
        Language.ARABIC: "مرحبا بالعالم",
        Language.CHINESE: "你好，世界",
        Language.ENGLISH: "Hello, world",
        Language.FRENCH: "Bonjour, monde",
        Language.HINDI: "नमस्ते दुनिया",
        Language.SPANISH: "Hola mundo",
    }
    return greetings.get(to_language)


@workflow.defn
class GreetingWorkflow:
    def __init__(self) -> None:
        self.lock = asyncio.Lock()
        self.approved_for_release = False
        self.greetings = {
            Language.CHINESE: "你好，世界",
            Language.ENGLISH: "Hello, world",
        }
        self.language = Language.ENGLISH

    @workflow.run
    async def run(self) -> str:
        await workflow.wait_condition(lambda: self.approved_for_release)
        await workflow.wait_condition(workflow.all_handlers_finished)
        return self.greetings[self.language]

    @workflow.signal
    def approve(self) -> None:
        self.approved_for_release = True

    @workflow.update
    async def set_language(self, language: Language) -> Language:
        if language not in self.greetings:
            # 👉 Use a lock here to ensure that multiple calls to set_language are processed in order.
            async with self.lock:
                greeting = await workflow.execute_activity(
                    call_greeting_service,
                    language,
                    start_to_close_timeout=timedelta(seconds=10),
                )
                if greeting is None:
                    # 👉 An update validator cannot be async, so cannot be used to check that the remote
                    # call_greeting_service supports the requested language. Raising ApplicationError
                    # will fail the Update, but the WorkflowExecutionUpdateAccepted event will still be
                    # added to history.
                    raise ApplicationError(
                        f"Greeting service does not support {language.name}"
                    )
                self.greetings[language] = greeting
        previous_language, self.language = self.language, language
        return previous_language
# @@@SNIPEND
