from temporalio.client import Client, TLSConfig
from temporalio.worker import Worker


async def connect_with_api_key(endpoint: str, namespace: str, api_key: str) -> Client:
    # endpoint is the gRPC Namespace endpoint, such as <namespace>.<account>.tmprl.cloud:7233.
    # namespace is your <namespace_id>.<account_id> combination.
    # @@@SNIPSTART python-client-cloud-api-key
    client = await Client.connect(
        endpoint,
        namespace=namespace,
        api_key=api_key,
        tls=True,
    )
    # @@@SNIPEND
    return client


async def rotate_client_cert(worker: Worker) -> None:
    # @@@SNIPSTART python-client-rotate-mtls-cert
    with open("client-cert-new.pem", "rb") as f:
        client_cert = f.read()
    with open("client-private-key-new.pem", "rb") as f:
        client_private_key = f.read()

    new_client = await Client.connect(
        "your-custom-namespace.tmprl.cloud:7233",
        namespace="<your-custom-namespace>.<account-id>",
        tls=TLSConfig(
            client_cert=client_cert,
            client_private_key=client_private_key,
        ),
    )

    # worker is the Worker instance already running against the old client
    worker.client = new_client
    # @@@SNIPEND
