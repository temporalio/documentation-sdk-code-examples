namespace TemporalioDocs.Workflows.ChildWorkflows;

using Temporalio.Workflows;

[Workflow]
public class MyChildWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync() => "Hello from the Child Workflow";
}
