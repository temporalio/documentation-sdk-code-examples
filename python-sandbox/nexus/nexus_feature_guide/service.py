"""The Nexus Service contract shared by the handler and the caller."""

from dataclasses import dataclass

import nexusrpc


@dataclass
class EchoInput:
    message: str


@dataclass
class EchoOutput:
    message: str


@dataclass
class HelloInput:
    name: str
    language: str


@dataclass
class HelloOutput:
    message: str


@nexusrpc.service
class MyNexusService:
    echo: nexusrpc.Operation[EchoInput, EchoOutput]
    hello: nexusrpc.Operation[HelloInput, HelloOutput]
    hello_with_args: nexusrpc.Operation[HelloInput, HelloOutput]
