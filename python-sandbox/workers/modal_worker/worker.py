# @@@SNIPSTART python-modal-worker-worker
from datetime import timedelta

import modal
from temporalio.common import WorkerDeploymentVersion
from temporalio.worker_integrations.modal.function import ModalWorkerConfig, run_worker

from modal_worker.activities import greet
from modal_worker.workflows import GreetingWorkflow

FUNCTION_TIMEOUT = timedelta(minutes=5)
VERSION = WorkerDeploymentVersion(deployment_name="my-app", build_id="build-1")

image = (
    modal.Image.debian_slim(python_version="3.12")
    .pip_install("temporalio-modal-function-worker==0.0.1")
    .add_local_python_source("modal_worker")
)
app = modal.App("temporal-worker", image=image)


def configure(config: ModalWorkerConfig) -> None:
    config.client_connect_config["tls"] = True
    config.worker_config["task_queue"] = "my-task-queue"
    config.worker_config["workflows"] = [GreetingWorkflow]
    config.worker_config["activities"] = [greet]


@app.function(
    timeout=int(FUNCTION_TIMEOUT.total_seconds()),
    secrets=[modal.Secret.from_name("temporal")],
)
async def temporal_worker() -> None:
    await run_worker(
        VERSION,
        configure,
        modal_function_timeout=FUNCTION_TIMEOUT,
    )
# @@@SNIPEND
