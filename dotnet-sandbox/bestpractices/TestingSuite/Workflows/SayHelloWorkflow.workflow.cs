// @@@SNIPSTART dotnet-testing-say-hello-workflow
namespace TemporalioDocs.BestPractices.TestingSuite;

using Temporalio.Workflows;

[Workflow]
public class SayHelloWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync(string name)
    {
        return $"Hello, {name}!";
    }
}
// @@@SNIPEND
