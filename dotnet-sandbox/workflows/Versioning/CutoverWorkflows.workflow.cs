namespace TemporalioDocs.Workflows.Versioning;

using Temporalio.Workflows;

// @@@SNIPSTART dotnet-versioning-cutover-workflows
[Workflow]
public class SayHelloWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync(string name)
    {
        // This method contains the original code
        return $"Hello, {name}!";
    }
}

[Workflow]
public class SayHelloWorkflowV2
{
    [WorkflowRun]
    public async Task<string> RunAsync(string name)
    {
        // This method contains the updated code
        return $"Hello, {name}! Welcome back.";
    }
}
// @@@SNIPEND
