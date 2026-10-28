namespace TemporalioDocs.Activities.StandaloneActivities;

using Temporalio.Client;

public static class ActivityHandleExamples
{
    public static async Task<string> GetHandlesAsync(ITemporalClient client, string runId)
    {
        // @@@SNIPSTART dotnet-standalone-activities-get-handle
        // Without a known result type
        var handle = client.GetActivityHandle("my-activity-id", runId: "the-run-id");

        // With a known result type
        var typedHandle = client.GetActivityHandle<string>("my-activity-id", runId: "the-run-id");
        // @@@SNIPEND

        var started = client.GetActivityHandle<string>("standalone-activity-id", runId: runId);
        // @@@SNIPSTART dotnet-standalone-activities-get-result
        var result = await started.GetResultAsync();
        // @@@SNIPEND
        return result;
    }
}
