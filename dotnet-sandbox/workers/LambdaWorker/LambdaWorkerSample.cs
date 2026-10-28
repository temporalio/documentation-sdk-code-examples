namespace TemporalioDocs.Workers.LambdaWorker;

using Temporalio.Worker;

// Shared settings for the OpenTelemetry Lambda function.
public static class LambdaWorkerSample
{
    public const string DeploymentName = "my-app";
    public const string BuildId = "build-1";
    public const string TaskQueue = "my-task-queue";

    public static void ConfigureWorkerOptions(TemporalWorkerOptions options)
    {
        options.TaskQueue = TaskQueue;
        options.AddWorkflow<SampleWorkflow>();
        options.AddActivity(Activities.HelloActivity);
    }
}
