namespace TemporalioDocs.Workers.LambdaWorker;

using Temporalio.Activities;

public static class Activities
{
    [Activity]
    public static string HelloActivity(string name) => $"Hello, {name}!";
}
