namespace TemporalioDocs.Activities.StandaloneActivities;

// @@@SNIPSTART dotnet-standalone-activities-start
using Temporalio.Client;
using Temporalio.Common.EnvConfig;

public static class StartActivityClient
{
    public static async Task RunAsync()
    {
        var connectOptions = ClientEnvConfig.LoadClientConnectOptions();
        connectOptions.TargetHost ??= "localhost:7233";
        var client = await TemporalClient.ConnectAsync(connectOptions);

        var handle = await client.StartActivityAsync(
            () => MyActivities.ComposeGreetingAsync(new ComposeGreetingInput("Hello", "World")),
            new("standalone-activity-id", "standalone-activity-sample")
            {
                ScheduleToCloseTimeout = TimeSpan.FromSeconds(10),
            });
        Console.WriteLine($"Started activity: {handle.Id}");

        // Wait for the result later
        var result = await handle.GetResultAsync();
        Console.WriteLine($"Activity result: {result}");
    }
}
// @@@SNIPEND
