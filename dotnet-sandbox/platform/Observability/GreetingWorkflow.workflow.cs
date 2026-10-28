namespace TemporalioDocs.Platform.Observability;

using Microsoft.Extensions.Logging;
using Temporalio.Workflows;

[Workflow]
public class GreetingWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync(string name)
    {
        // @@@SNIPSTART dotnet-observability-workflow-logger
        Workflow.Logger.LogInformation("Given name: {Name}", name);
        // @@@SNIPEND
        return $"Hello, {name}!";
    }
}
