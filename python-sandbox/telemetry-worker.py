import asyncio
from temporalio.client import Client
from temporalio.worker import Worker
from temporalio import workflow
from temporalio.runtime import Runtime, TelemetryConfig, PrometheusConfig

with workflow.unsafe.imports_passed_through():
    from workflows import SayHelloWorkflow
    from activities import greet

async def main():
    new_runtime = Runtime(
        telemetry=TelemetryConfig(
            metrics=PrometheusConfig(bind_address="0.0.0.0:9000"),
            global_tags={
                "team": "content-platform",
                "service": "checkout",
                "cost_center": "cc-1042",
                "environment": "production",
            },
        )
    )
    
    client = await Client.connect("localhost:7233", runtime=new_runtime)
    worker = Worker(
        client,
        task_queue="my-task-queue",
        workflows=[SayHelloWorkflow],
        activities=[greet],
    )
    print("Worker started.")
    await worker.run()

if __name__ == "__main__":
    asyncio.run(main())