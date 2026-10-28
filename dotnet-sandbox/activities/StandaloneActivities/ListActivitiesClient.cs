namespace TemporalioDocs.Activities.StandaloneActivities;

// @@@SNIPSTART dotnet-standalone-activities-list
using Temporalio.Client;
using Temporalio.Common.EnvConfig;

public static class ListActivitiesClient
{
    public static async Task RunAsync()
    {
        var connectOptions = ClientEnvConfig.LoadClientConnectOptions();
        connectOptions.TargetHost ??= "localhost:7233";
        var client = await TemporalClient.ConnectAsync(connectOptions);

        await foreach (var info in client.ListActivitiesAsync(
            "TaskQueue = 'standalone-activity-sample'"))
        {
            Console.WriteLine(
                $"ActivityID: {info.ActivityId}, Type: {info.ActivityType}, Status: {info.Status}");
        }
    }
}
// @@@SNIPEND
