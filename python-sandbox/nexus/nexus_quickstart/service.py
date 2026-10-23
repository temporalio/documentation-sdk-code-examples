# @@@SNIPSTART python-nexus-quickstart-service
from dataclasses import dataclass

import nexusrpc


@dataclass
class MyInput:
    name: str


@nexusrpc.service
class SayHelloNexusService:
    say_hello: nexusrpc.Operation[MyInput, str]
# @@@SNIPEND
