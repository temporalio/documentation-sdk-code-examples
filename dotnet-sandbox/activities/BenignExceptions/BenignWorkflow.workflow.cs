namespace TemporalioDocs.Activities.BenignExceptions;

using Temporalio.Workflows;

[Workflow]
public class BenignWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync() =>
        await Workflow.ExecuteActivityAsync(
            (MyActivities a) => a.MyActivityAsync(),
            new()
            {
                StartToCloseTimeout = TimeSpan.FromMinutes(1),
                RetryPolicy = new() { MaximumAttempts = 2, InitialInterval = TimeSpan.FromSeconds(1) },
            });
}
