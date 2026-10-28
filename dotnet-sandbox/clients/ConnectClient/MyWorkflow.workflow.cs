namespace TemporalioDocs.Client.ConnectClient;

using Temporalio.Workflows;

[Workflow]
public class MyWorkflow
{
    [WorkflowRun]
    public Task<string> RunAsync() => Task.FromResult("Hello from MyWorkflow");
}
