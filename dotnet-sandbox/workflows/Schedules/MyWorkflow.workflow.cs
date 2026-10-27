namespace TemporalioDocs.Workflows.Schedules;

using Temporalio.Workflows;

[Workflow]
public class MyWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync() => "Hello from a Scheduled Workflow";
}
