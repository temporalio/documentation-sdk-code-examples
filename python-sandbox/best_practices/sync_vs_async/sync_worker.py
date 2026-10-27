import asyncio
from concurrent.futures import ThreadPoolExecutor

from temporalio.client import Client
from temporalio.worker import Worker

from sync_vs_async.sync_class_activities import TranslateActivities
from sync_vs_async.workflow import GreetingWorkflow


async def main():
    client = await Client.connect("localhost:7233")
    activities = TranslateActivities()
    # @@@SNIPSTART python-sync-vs-async-sync-worker
    with ThreadPoolExecutor(max_workers=42) as executor:
        worker = Worker(
            client,
            task_queue="greeting-tasks",
            workflows=[GreetingWorkflow],
            activities=[activities.greet_in_spanish],
            activity_executor=executor,
        )
        # @@@SNIPEND
        await worker.run()


if __name__ == "__main__":
    asyncio.run(main())
