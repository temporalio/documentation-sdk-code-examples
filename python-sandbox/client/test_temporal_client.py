import uuid
from pathlib import Path

import pytest
import pytest_asyncio
from temporalio.client import Client, TLSConfig
from temporalio.envconfig import ClientConfigProfile
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from temporal_client import (
    cloud_connect,
    get_workflow_result,
    load_default,
    load_from_env,
    load_profile_with_overrides,
    mtls_connect,
    start_workflow,
)
from temporal_client.your_workflows import YourWorkflow

SAMPLE_DIR = Path(__file__).parent / "temporal_client"
REAL_CONNECT = Client.connect


@pytest_asyncio.fixture
async def env():
    async with await WorkflowEnvironment.start_local() as env:
        yield env


@pytest.fixture
def connect_calls(monkeypatch, env):
    """Record each Client.connect call and send it to the test server instead."""
    calls = []
    target = env.client.service_client.config.target_host

    async def connect(target_host, **kwargs):
        calls.append((target_host, dict(kwargs)))
        kwargs.pop("tls", None)
        kwargs.pop("api_key", None)
        kwargs["namespace"] = env.client.namespace
        # A Worker can only switch to a client on the same Runtime as its current one.
        kwargs["runtime"] = env.client.service_client.config.runtime
        return await REAL_CONNECT(target, **kwargs)

    monkeypatch.setattr(Client, "connect", connect)
    return calls


@pytest.mark.parametrize(
    "file, profile, expected",
    [
        ("config.toml", "default", {"address": "localhost:7233", "grpc_meta": {"my-custom-header": "development-value", "trace-id": "dev-trace-123"}}),
        ("config.toml", "prod", {"api_key": "your-api-key-here", "tls_cert": Path("/etc/temporal/certs/client.pem")}),
        ("cloud_api_key.toml", "staging", {"api_key": "your-api-key-here"}),
        ("cloud_mtls.toml", "staging", {"tls_cert": b"your-tls-client-cert-data"}),
    ],
)
def test_config_files_load_strictly(file, profile, expected):
    loaded = ClientConfigProfile.load(
        profile=profile,
        config_source=SAMPLE_DIR / file,
        disable_env=True,
        config_file_strict=True,
    )
    if "address" in expected:
        assert loaded.address == expected["address"]
    if "grpc_meta" in expected:
        assert dict(loaded.grpc_meta) == expected["grpc_meta"]
    if "api_key" in expected:
        assert loaded.api_key == expected["api_key"]
    if "tls_cert" in expected:
        assert loaded.tls.client_cert == expected["tls_cert"]


@pytest.mark.asyncio
async def test_load_default_profile(connect_calls, capsys, monkeypatch):
    monkeypatch.delenv("TEMPORAL_ADDRESS", raising=False)
    await load_default.main()
    out = capsys.readouterr().out
    assert "✅ Client connected successfully!" in out
    target, kwargs = connect_calls[0]
    assert target == "localhost:7233"
    assert kwargs["rpc_metadata"]["my-custom-header"] == "development-value"


@pytest.mark.asyncio
async def test_load_from_env(connect_calls, capsys, monkeypatch):
    monkeypatch.setenv("TEMPORAL_ADDRESS", "localhost:7233")
    monkeypatch.setenv("TEMPORAL_NAMESPACE", "default")
    monkeypatch.setenv("TEMPORAL_CONFIG_FILE", str(SAMPLE_DIR / "does-not-exist.toml"))
    await load_from_env.main()
    assert "✅ Client connected successfully!" in capsys.readouterr().out
    assert connect_calls[0][0] == "localhost:7233"


@pytest.mark.asyncio
async def test_load_profile_with_overrides(connect_calls, capsys, monkeypatch):
    monkeypatch.delenv("TEMPORAL_ADDRESS", raising=False)
    await load_profile_with_overrides.main()
    assert "✅ Client connected successfully!" in capsys.readouterr().out
    # The staging profile says localhost:9999; the sample overrides it.
    assert connect_calls[0][0] == "localhost:7233"


@pytest.mark.asyncio
async def test_start_workflow_and_get_result(env, connect_calls, capsys):
    async with Worker(env.client, task_queue="your-task-queue", workflows=[YourWorkflow]):
        await start_workflow.main()
        assert "Result: Hello, your name!" in capsys.readouterr().out
        await get_workflow_result.main()
        assert "Result: Hello, your name!" in capsys.readouterr().out


@pytest.mark.asyncio
async def test_connect_to_cloud_with_api_key(connect_calls):
    await cloud_connect.connect_with_api_key(
        "my-namespace.a1b2c.tmprl.cloud:7233", "my-namespace.a1b2c", "my-api-key"
    )
    target, kwargs = connect_calls[0]
    assert target == "my-namespace.a1b2c.tmprl.cloud:7233"
    assert kwargs == {"namespace": "my-namespace.a1b2c", "api_key": "my-api-key", "tls": True}


@pytest.mark.asyncio
async def test_connect_with_mtls_and_rotate_cert(env, connect_calls, monkeypatch, tmp_path):
    for name in ["client-cert", "client-private-key", "client-cert-new", "client-private-key-new"]:
        (tmp_path / f"{name}.pem").write_bytes(f"{name} bytes".encode())
    monkeypatch.chdir(tmp_path)

    await mtls_connect.main()
    tls = connect_calls[0][1]["tls"]
    assert isinstance(tls, TLSConfig)
    assert tls.client_cert == b"client-cert bytes"
    assert tls.client_private_key == b"client-private-key bytes"

    async with Worker(env.client, task_queue=str(uuid.uuid4()), workflows=[YourWorkflow]) as worker:
        await cloud_connect.rotate_client_cert(worker)
        assert connect_calls[1][1]["tls"].client_cert == b"client-cert-new bytes"
        assert worker.client is not env.client
