namespace TemporalioDocs.Workflows.Timeouts;

using Temporalio.Workflows;

[Workflow]
public class MyWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync() => "Hello, Temporal!";
}
