from temporalio.client import Client
from temporalio.worker import Interceptor, Worker

from interceptors.workflows import GreetingWorkflow


class SomeWorkerInterceptor(Interceptor):
    """An interceptor that passes every call through unchanged."""


async def connect_with_tracing() -> Client:
    from temporalio.contrib.opentelemetry import TracingInterceptor

    # @@@SNIPSTART python-interceptors-register-client
    client = await Client.connect(
        "localhost:7233",
        interceptors=[TracingInterceptor()],
    )
    # @@@SNIPEND
    return client


def create_worker(client: Client) -> Worker:
    # @@@SNIPSTART python-interceptors-register-worker
    worker = Worker(
        client,
        task_queue="my-task-queue",
        interceptors=[SomeWorkerInterceptor()],
        workflows=[GreetingWorkflow],
    )
    # @@@SNIPEND
    return worker
