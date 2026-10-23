# @@@SNIPSTART python-schedules-backfill
import asyncio
from datetime import datetime, timedelta, timezone

from temporalio.client import Client, ScheduleBackfill, ScheduleOverlapPolicy

async def main():
    client = await Client.connect("localhost:7233")
    handle = client.get_schedule_handle(
        "workflow-schedule-id",
    )
    now = datetime.now(timezone.utc)
    await handle.backfill(
        ScheduleBackfill(
            start_at=now - timedelta(minutes=10),
            end_at=now - timedelta(minutes=9),
            overlap=ScheduleOverlapPolicy.ALLOW_ALL,
        ),
    )

    print(f"Backfilled Schedule: {handle.id}")


if __name__ == "__main__":
    asyncio.run(main())
# @@@SNIPEND
