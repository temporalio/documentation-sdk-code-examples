// @@@SNIPSTART dotnet-nexus-quickstart-caller-workflow
namespace MyNamespace;

using Temporalio.Workflows;

[Workflow]
public class CallerWorkflow
{
    public static readonly string CallerTaskQueue = "my-caller-task-queue";

    [WorkflowRun]
    public async Task<string> RunAsync(string name)
    {
        return await Workflow
            .CreateNexusWorkflowClient<ISayHelloNexusService>(
                ISayHelloNexusService.EndpointName)
            .ExecuteNexusOperationAsync(svc => svc.SayHello(new(name)));
    }
}
// @@@SNIPEND
