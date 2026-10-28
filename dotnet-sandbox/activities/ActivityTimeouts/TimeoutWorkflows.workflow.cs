namespace TemporalioDocs.Activities.ActivityTimeouts;

using Temporalio.Workflows;

[Workflow]
public class StartToCloseWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync(string name)
    {
        var param = new MyActivityParams("Hello", name);
        // @@@SNIPSTART dotnet-activity-timeouts-start-to-close
        return await Workflow.ExecuteActivityAsync(
            (MyActivities a) => a.MyActivity(param),
            new() { StartToCloseTimeout = TimeSpan.FromMinutes(5) });
        // @@@SNIPEND
    }
}

[Workflow]
public class RetryPolicyWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync(string name)
    {
        var param = new MyActivityParams("Hello", name);
        // @@@SNIPSTART dotnet-activity-timeouts-retry-policy
        return await Workflow.ExecuteActivityAsync(
            (MyActivities a) => a.MyActivity(param),
            new()
            {
                StartToCloseTimeout = TimeSpan.FromMinutes(5),
                RetryPolicy = new() { MaximumInterval = TimeSpan.FromSeconds(10) },
            });
        // @@@SNIPEND
    }
}

[Workflow]
public class HeartbeatTimeoutWorkflow
{
    [WorkflowRun]
    public async Task RunAsync(string name)
    {
        var param = new MyActivityParams("Hello", name);
        // @@@SNIPSTART dotnet-activity-timeouts-heartbeat-timeout
        await Workflow.ExecuteActivityAsync(
            (MyActivities a) => a.MyActivity(param),
            new()
            {
                StartToCloseTimeout = TimeSpan.FromMinutes(5),
                HeartbeatTimeout = TimeSpan.FromSeconds(30),
            });
        // @@@SNIPEND
    }
}
