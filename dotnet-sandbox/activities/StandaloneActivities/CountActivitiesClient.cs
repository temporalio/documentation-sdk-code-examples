namespace TemporalioDocs.Activities.StandaloneActivities;

// @@@SNIPSTART dotnet-standalone-activities-count
using Temporalio.Client;
using Temporalio.Common.EnvConfig;

public static class CountActivitiesClient
{
    public static async Task RunAsync()
    {
        var connectOptions = ClientEnvConfig.LoadClientConnectOptions();
        connectOptions.TargetHost ??= "localhost:7233";
        var client = await TemporalClient.ConnectAsync(connectOptions);

        var resp = await client.CountActivitiesAsync(
            "TaskQueue = 'standalone-activity-sample'");
        Console.WriteLine($"Total activities: {resp.Count}");
    }
}
// @@@SNIPEND
