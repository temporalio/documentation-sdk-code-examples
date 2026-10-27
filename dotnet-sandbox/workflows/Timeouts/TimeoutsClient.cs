namespace TemporalioDocs.Workflows.Timeouts;

using Temporalio.Client;

public static class TimeoutsClient
{
    public static async Task<string> ExecuteWithTimeoutAsync(ITemporalClient client)
    {
        // @@@SNIPSTART dotnet-workflow-timeouts-execution-timeout
        var result = await client.ExecuteWorkflowAsync(
            (MyWorkflow wf) => wf.RunAsync(),
            new(id: "my-workflow-id", taskQueue: "my-task-queue")
            {
                ExecutionTimeout = TimeSpan.FromMinutes(5),
            });
        // @@@SNIPEND
        return result;
    }

    public static async Task<string> ExecuteWithRetryPolicyAsync(ITemporalClient client)
    {
        // @@@SNIPSTART dotnet-workflow-timeouts-retry-policy
        var result = await client.ExecuteWorkflowAsync(
            (MyWorkflow wf) => wf.RunAsync(),
            new(id: "my-workflow-id", taskQueue: "my-task-queue")
            {
                RetryPolicy = new() { MaximumInterval = TimeSpan.FromSeconds(10) },
            });
        // @@@SNIPEND
        return result;
    }
}
