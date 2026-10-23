import asyncio

from temporalio.client import Client, WorkflowUpdateStage

from message_passing.greeting_workflow import (
    ApproveInput,
    GetLanguagesInput,
    GreetingWorkflow,
    Language,
)


async def main():
    # @@@SNIPSTART python-message-passing-start-workflow
    client = await Client.connect("localhost:7233")
    workflow_handle = await client.start_workflow(
        GreetingWorkflow.run, id="greeting-workflow-1234", task_queue="my-task-queue"
    )
    # @@@SNIPEND

    # @@@SNIPSTART python-message-passing-send-query
    supported_languages = await workflow_handle.query(
        GreetingWorkflow.get_languages, GetLanguagesInput(include_unsupported=False)
    )
    # @@@SNIPEND
    print(f"Supported languages: {supported_languages}")

    # @@@SNIPSTART python-message-passing-execute-update
    previous_language = await workflow_handle.execute_update(
        GreetingWorkflow.set_language, Language.CHINESE
    )
    # @@@SNIPEND
    print(f"Previous language: {previous_language.name}")

    # @@@SNIPSTART python-message-passing-start-update
    # Wait until the update is accepted
    update_handle = await workflow_handle.start_update(
        GreetingWorkflow.set_language,
        Language.ENGLISH,
        wait_for_stage=WorkflowUpdateStage.ACCEPTED,
    )
    # Wait until the update is completed
    update_result = await update_handle.result()
    # @@@SNIPEND
    print(f"Previous language: {update_result.name}")

    # @@@SNIPSTART python-message-passing-send-signal
    await workflow_handle.signal(GreetingWorkflow.approve, ApproveInput(name="me"))
    # @@@SNIPEND

    print(f"Workflow result: {await workflow_handle.result()}")


if __name__ == "__main__":
    asyncio.run(main())
