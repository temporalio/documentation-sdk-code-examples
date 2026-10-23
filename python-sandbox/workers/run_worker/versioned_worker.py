import asyncio

from temporalio.client import Client
from temporalio.common import VersioningBehavior, WorkerDeploymentVersion
from temporalio.worker import Worker, WorkerDeploymentConfig

from run_worker.hello import HelloWorkflow, some_activity


def create_versioned_worker(client: Client) -> Worker:
    # @@@SNIPSTART python-run-worker-versioned
    worker = Worker(
        client,
        task_queue="my-task-queue",
        workflows=[HelloWorkflow],
        activities=[some_activity],
        deployment_config=WorkerDeploymentConfig(
            version=WorkerDeploymentVersion(
                deployment_name="my-app",
                build_id="1.0",
            ),
            use_worker_versioning=True,
            default_versioning_behavior=VersioningBehavior.PINNED,
        ),
    )
    # @@@SNIPEND
    return worker


async def main():
    client = await Client.connect("localhost:7233")
    await create_versioned_worker(client).run()


if __name__ == "__main__":
    asyncio.run(main())
