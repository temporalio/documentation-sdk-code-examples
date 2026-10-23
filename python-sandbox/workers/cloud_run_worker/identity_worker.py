# @@@SNIPSTART python-cloud-run-identity
import asyncio

from temporalio.client import Client
from temporalio.contrib.gcp.cloud_run.id import CloudRunIdPlugin
from temporalio.worker import Worker

from cloud_run_worker.greeting_workflow import GreetingWorkflow, compose_greeting
from cloud_run_worker.settings import load_settings


async def main() -> None:
    settings = load_settings()
    client = await Client.connect(
        settings.address,
        namespace=settings.namespace,
        api_key=settings.api_key,
        tls=settings.tls,
        plugins=[CloudRunIdPlugin()],
    )
    # Workers created from this Client inherit its identity.
    worker = Worker(
        client,
        task_queue=settings.task_queue,
        workflows=[GreetingWorkflow],
        activities=[compose_greeting],
    )
    await worker.run()


if __name__ == "__main__":
    asyncio.run(main())
# @@@SNIPEND
