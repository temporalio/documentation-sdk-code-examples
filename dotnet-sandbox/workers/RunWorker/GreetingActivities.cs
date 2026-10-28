namespace TemporalioDocs.Workers.RunWorker;

using Temporalio.Activities;

public static class GreetingActivities
{
    [Activity]
    public static string ComposeGreeting(string name) => $"Hello, {name}!";
}
