import asyncio
import threading
import urllib.parse
import uuid
from concurrent.futures import ThreadPoolExecutor
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

import aiohttp
import pytest
from temporalio.client import Client, WorkflowFailureError
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from sync_vs_async import async_activities, sync_class_activities, sync_function_activities, sync_worker
from sync_vs_async.workflow import GreetingWorkflow

pytestmark = pytest.mark.asyncio


class TranslationHandler(BaseHTTPRequestHandler):
    """Stand-in for the translation microservice the Activities call."""

    def do_GET(self):
        url = urllib.parse.urlparse(self.path)
        name = urllib.parse.parse_qs(url.query).get("name", [""])[0]
        if url.path == "/get-spanish-greeting" and name:
            status, body = 200, f"¡Hola, {name}!"
        else:
            status, body = 400, "missing name"
        self.send_response(status)
        self.end_headers()
        self.wfile.write(body.encode())

    def log_message(self, *args):
        pass


@pytest.fixture(scope="module", autouse=True)
def translation_service():
    server = ThreadingHTTPServer(("localhost", 9999), TranslationHandler)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    yield
    server.shutdown()


async def run_greeting(env, activity, name="Ana", activity_executor=None):
    task_queue = str(uuid.uuid4())
    async with Worker(
        env.client,
        task_queue=task_queue,
        workflows=[GreetingWorkflow],
        activities=[activity],
        activity_executor=activity_executor,
    ):
        return await env.client.execute_workflow(
            GreetingWorkflow.run, name, id=str(uuid.uuid4()), task_queue=task_queue
        )


async def test_sync_activities():
    async with await WorkflowEnvironment.start_local() as env:
        with ThreadPoolExecutor(max_workers=4) as executor:
            activities = sync_class_activities.TranslateActivities()
            assert await run_greeting(env, activities.greet_in_spanish, activity_executor=executor) == "¡Hola, Ana!"
            assert (
                await run_greeting(env, sync_function_activities.greet_in_spanish, activity_executor=executor)
                == "¡Hola, Ana!"
            )


async def test_sync_worker(monkeypatch):
    async with await WorkflowEnvironment.start_local() as env:
        async def connect(*args, **kwargs):
            return env.client

        monkeypatch.setattr(Client, "connect", connect)
        worker = asyncio.create_task(sync_worker.main())
        try:
            result = await env.client.execute_workflow(
                GreetingWorkflow.run, "Luis", id=str(uuid.uuid4()), task_queue="greeting-tasks"
            )
            assert result == "¡Hola, Luis!"
        finally:
            worker.cancel()
            with pytest.raises(asyncio.CancelledError):
                await worker


async def test_async_activities():
    async with await WorkflowEnvironment.start_local() as env:
        async with aiohttp.ClientSession() as session:
            activities = async_activities.TranslateActivities(session)
            assert await run_greeting(env, activities.greet_in_spanish) == "¡Hola, Ana!"
            # A 4xx response is non-retryable, so the Workflow fails right away.
            with pytest.raises(WorkflowFailureError):
                await run_greeting(env, activities.greet_in_spanish, name="")
