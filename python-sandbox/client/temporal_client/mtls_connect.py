# @@@SNIPSTART python-client-cloud-mtls
from temporalio.client import Client, TLSConfig


async def main():
    with open("client-cert.pem", "rb") as f:
        client_cert = f.read()
    with open("client-private-key.pem", "rb") as f:
        client_private_key = f.read()
    client = await Client.connect(
        "your-custom-namespace.tmprl.cloud:7233",
        namespace="<your-custom-namespace>.<account-id>",
        tls=TLSConfig(
            client_cert=client_cert,
            client_private_key=client_private_key,
            # domain=domain, # TLS domain
            # server_root_ca_cert=server_root_ca_cert, # ROOT CA to validate the server cert
        ),
    )
    # @@@SNIPEND
    return client
