namespace TemporalioDocs.Activities.StandaloneActivities;

using Temporalio.Client;

public static class ExecuteActivityExamples
{
    public static async Task<(string Typed, string Untyped)> ExecuteBothWaysAsync(ITemporalClient client)
    {
        // @@@SNIPSTART dotnet-standalone-activities-execute-options
        // Using a lambda expression (type-safe)
        var result = await client.ExecuteActivityAsync(
            () => MyActivities.ComposeGreetingAsync(new ComposeGreetingInput("Hello", "World")),
            new("standalone-activity-id", "standalone-activity-sample")
            {
                ScheduleToCloseTimeout = TimeSpan.FromSeconds(10),
            });

        // Using a string type name
        var resultByName = await client.ExecuteActivityAsync<string>(
            "ComposeGreeting",
            new object?[] { new ComposeGreetingInput("Hello", "World") },
            new("standalone-activity-id", "standalone-activity-sample")
            {
                ScheduleToCloseTimeout = TimeSpan.FromSeconds(10),
            });
        // @@@SNIPEND
        return (result, resultByName);
    }
}
