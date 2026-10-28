namespace TemporalioDocs.Workers.CloudRunWorker;

using Temporalio.Activities;

public static class GreetingActivities
{
    [Activity]
    public static string ComposeGreeting(string name) => $"Hello, {name}!";

    // @@@SNIPSTART dotnet-cloud-run-worker-heartbeat
    [Activity]
    public static string Process(IReadOnlyList<string> items)
    {
        for (var i = 0; i < items.Count; i++)
        {
            ActivityExecutionContext.Current.Heartbeat(i);
            // ... process items[i]
        }
        return "done";
    }
    // @@@SNIPEND
}
