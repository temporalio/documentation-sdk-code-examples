from temporalio import workflow

with workflow.unsafe.imports_passed_through():
    from nexus_feature_guide.service import HelloInput, HelloOutput

GREETINGS = {"en": "Hello", "es": "Hola", "fr": "Bonjour"}


@workflow.defn
class HelloHandlerWorkflow:
    @workflow.run
    async def run(self, input: HelloInput) -> HelloOutput:
        return HelloOutput(message=f"{GREETINGS[input.language]} {input.name}")


@workflow.defn
class HelloArgsHandlerWorkflow:
    """Takes the Operation input as separate arguments."""

    @workflow.run
    async def run(self, name: str, language: str) -> HelloOutput:
        return HelloOutput(message=f"{GREETINGS[language]} {name}")
