namespace TemporalioDocs.Activities.StandaloneActivities;

// @@@SNIPSTART dotnet-standalone-activities-execute
using Temporalio.Client;
using Temporalio.Common.EnvConfig;

public static class StandaloneClient
{
    public static async Task RunAsync()
    {
        var connectOptions = ClientEnvConfig.LoadClientConnectOptions();
        connectOptions.TargetHost ??= "localhost:7233";
        var client = await TemporalClient.ConnectAsync(connectOptions);

        var result = await client.ExecuteActivityAsync(
            () => MyActivities.ComposeGreetingAsync(new ComposeGreetingInput("Hello", "World")),
            new("standalone-activity-id", "standalone-activity-sample")
            {
                ScheduleToCloseTimeout = TimeSpan.FromSeconds(10),
            });
        Console.WriteLine($"Activity result: {result}");
    }
}
// @@@SNIPEND
