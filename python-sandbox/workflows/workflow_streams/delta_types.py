from dataclasses import dataclass
from typing import Iterator


@dataclass
class Delta:
    text: str


def generate_deltas(order_id: str) -> Iterator[Delta]:
    for word in ["Your", "order", order_id, "is", "on", "its", "way."]:
        yield Delta(text=word)


def first_phase() -> Iterator[Delta]:
    yield Delta(text="phase one")


async def record_phase_one_complete() -> str:
    # Pretend that we are recording a checkpoint in an external system.
    return "checkpoint-1"


def second_phase(checkpoint_id: str) -> Iterator[Delta]:
    yield Delta(text=f"phase two after {checkpoint_id}")
