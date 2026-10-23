# @@@SNIPSTART python-cloud-run-versioned-worker
import asyncio
import os

from temporalio.client import Client
from temporalio.common import VersioningBehavior, WorkerDeploymentVersion
from temporalio.envconfig import ClientConfig
from temporalio.worker import Worker, WorkerDeploymentConfig

from cloud_run_worker.my_activities import my_activity
from cloud_run_worker.my_workflows import MyWorkflow


async def main() -> None:
    client = await Client.connect(**ClientConfig.load_client_connect_config())

    worker = Worker(
        client,
        task_queue=os.environ["TEMPORAL_TASK_QUEUE"],
        workflows=[MyWorkflow],
        activities=[my_activity],
        deployment_config=WorkerDeploymentConfig(
            version=WorkerDeploymentVersion(
                deployment_name="my-app",
                build_id="build-1",
            ),
            use_worker_versioning=True,
            default_versioning_behavior=VersioningBehavior.PINNED,
        ),
    )
    await worker.run()


if __name__ == "__main__":
    asyncio.run(main())
# @@@SNIPEND
