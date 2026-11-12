using System.Text;
using Temporalio.Api.Common.V1;
using Temporalio.Client;
using Temporalio.Common.EnvConfig;
using Temporalio.Converters;
using Temporalio.Worker;

namespace TemporalioDocs.ExternalStorageExample;

public static class ExampleRunner
{
    public static async Task<TemporalClient> ConnectAsync(IStorageDriver driver)
    {
        // @@@SNIPSTART dotnet-external-storage-client-setup
        var dataConverter = DataConverter.Default with
        {
            ExternalStorage = new ExternalStorage(driver),
        };
        var options = ClientEnvConfig.LoadClientConnectOptions();
        options.TargetHost ??= "localhost:7233";
        options.DataConverter = dataConverter;
        var client = await TemporalClient.ConnectAsync(options);
        // @@@SNIPEND
        return client;
    }

    public static async Task<RoundTripResult> RunAsync(ITemporalClient client)
    {
        var taskQueue = $"external-storage-{Guid.NewGuid():N}";
        // @@@SNIPSTART dotnet-external-storage-worker-setup
        using var worker = new TemporalWorker(
            client,
            new TemporalWorkerOptions(taskQueue)
                .AddWorkflow<LargePayloadWorkflow>()
                .AddActivity(LargePayloadActivities.Echo));
        // @@@SNIPEND
        return await worker.ExecuteAsync(async () =>
        {
            var input = new string('x', 3 * 1024 * 1024);
            var workflowId = $"external-storage-{Guid.NewGuid():N}";
            var handle = await client.StartWorkflowAsync(
                (LargePayloadWorkflow workflow) => workflow.RunAsync(input),
                new(id: workflowId, taskQueue: taskQueue)
                {
                    ExecutionTimeout = TimeSpan.FromMinutes(1),
                });
            var output = await handle.GetResultAsync();
            if (output != input)
                throw new InvalidOperationException("The retrieved payload differs from the input.");

            var history = await handle.FetchHistoryAsync();
            var payloads = new List<Payload>();
            foreach (var historyEvent in history.Events)
            {
                var values = historyEvent.WorkflowExecutionStartedEventAttributes?.Input
                    ?? historyEvent.ActivityTaskScheduledEventAttributes?.Input
                    ?? historyEvent.ActivityTaskCompletedEventAttributes?.Result
                    ?? historyEvent.WorkflowExecutionCompletedEventAttributes?.Result;
                if (values != null)
                    payloads.AddRange(values.Payloads_);
            }
            if (payloads.Count != 4 || payloads.Any(payload =>
                !payload.Metadata.TryGetValue("messageType", out var messageType)
                || messageType.ToStringUtf8() != "temporal.api.sdk.v1.ExternalStorageReference"
                || payload.CalculateSize() >= 1024))
                throw new InvalidOperationException("Expected four small External Storage references in Event History.");

            return new RoundTripResult(workflowId, Encoding.UTF8.GetByteCount(output),
                payloads.Count, payloads.Max(payload => payload.CalculateSize()), history.Events.Count);
        });
    }
}

public sealed record RoundTripResult(string WorkflowId, int ResultBytes,
    int OffloadedPayloadCount, int LargestReferenceBytes, int HistoryEventCount);
