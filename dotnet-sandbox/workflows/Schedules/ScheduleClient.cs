namespace TemporalioDocs.Workflows.Schedules;

using Temporalio.Api.Enums.V1;
using Temporalio.Client;
using Temporalio.Client.Schedules;

public static class ScheduleClient
{
    public static async Task CreateAsync()
    {
        // @@@SNIPSTART dotnet-schedules-create
        var client = await TemporalClient.ConnectAsync(new("localhost:7233"));

        var handle = await client.CreateScheduleAsync(
            "my-schedule-id",
            new(
                Action: ScheduleActionStartWorkflow.Create(
                    (MyWorkflow wf) => wf.RunAsync(),
                    new(id: "my-workflow-id", taskQueue: "my-task-queue")),
                Spec: new()
                {
                    Intervals = new List<ScheduleIntervalSpec> { new(Every: TimeSpan.FromDays(5)) },
                }));
        // @@@SNIPEND
    }

    public static async Task BackfillAsync()
    {
        // @@@SNIPSTART dotnet-schedules-backfill
        var client = await TemporalClient.ConnectAsync(new("localhost:7233"));

        var handle = client.GetScheduleHandle("my-schedule-id");
        var now = DateTime.UtcNow;
        await handle.BackfillAsync(new List<ScheduleBackfill>
        {
            new(
                StartAt: now - TimeSpan.FromDays(30),
                EndAt: now - TimeSpan.FromDays(20),
                Overlap: ScheduleOverlapPolicy.AllowAll),
        });
        // @@@SNIPEND
    }

    public static async Task DeleteAsync()
    {
        // @@@SNIPSTART dotnet-schedules-delete
        var client = await TemporalClient.ConnectAsync(new("localhost:7233"));

        var handle = client.GetScheduleHandle("my-schedule-id");
        await handle.DeleteAsync();
        // @@@SNIPEND
    }

    public static async Task DescribeAsync()
    {
        // @@@SNIPSTART dotnet-schedules-describe
        var client = await TemporalClient.ConnectAsync(new("localhost:7233"));

        var handle = client.GetScheduleHandle("my-schedule-id");
        var desc = await handle.DescribeAsync();
        Console.WriteLine("Schedule info: {0}", desc.Info);
        // @@@SNIPEND
    }

    public static async Task ListAsync()
    {
        // @@@SNIPSTART dotnet-schedules-list
        var client = await TemporalClient.ConnectAsync(new("localhost:7233"));
        await foreach (var desc in client.ListSchedulesAsync())
        {
            Console.WriteLine("Schedule info: {0}", desc.Info);
        }
        // @@@SNIPEND
    }

    public static async Task PauseAsync()
    {
        // @@@SNIPSTART dotnet-schedules-pause
        var client = await TemporalClient.ConnectAsync(new("localhost:7233"));

        var handle = client.GetScheduleHandle("my-schedule-id");
        await handle.PauseAsync("Pausing the schedule for now");
        // @@@SNIPEND
    }

    public static async Task TriggerAsync()
    {
        // @@@SNIPSTART dotnet-schedules-trigger
        var client = await TemporalClient.ConnectAsync(new("localhost:7233"));

        var handle = client.GetScheduleHandle("my-schedule-id");
        await handle.TriggerAsync();
        // @@@SNIPEND
    }

    public static async Task UpdateAsync()
    {
        // @@@SNIPSTART dotnet-schedules-update
        var client = await TemporalClient.ConnectAsync(new("localhost:7233"));

        var handle = client.GetScheduleHandle("my-schedule-id");
        await handle.UpdateAsync(input =>
        {
            var newAction = ScheduleActionStartWorkflow.Create(
                (MyWorkflow wf) => wf.RunAsync(),
                new(id: "my-workflow-id", taskQueue: "my-task-queue"));
            return new(input.Description.Schedule with { Action = newAction });
        });
        // @@@SNIPEND
    }

    public static async Task StartDelayAsync(ITemporalClient client)
    {
        // @@@SNIPSTART dotnet-schedules-start-delay
        var handle = await client.StartWorkflowAsync(
            (MyWorkflow wf) => wf.RunAsync(),
            new(id: "my-workflow-id", taskQueue: "my-task-queue")
            {
                StartDelay = TimeSpan.FromHours(3),
            });
        // @@@SNIPEND
    }
}
