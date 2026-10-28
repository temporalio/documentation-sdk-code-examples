// @@@SNIPSTART dotnet-standalone-activities-define
namespace TemporalioDocs.Activities.StandaloneActivities;

using Temporalio.Activities;

public static class MyActivities
{
    [Activity]
    public static Task<string> ComposeGreetingAsync(ComposeGreetingInput input) =>
        Task.FromResult($"{input.Greeting}, {input.Name}!");
}

public record ComposeGreetingInput(string Greeting, string Name);
// @@@SNIPEND
