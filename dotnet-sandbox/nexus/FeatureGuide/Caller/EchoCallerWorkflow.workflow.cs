// @@@SNIPSTART dotnet-nexus-feature-guide-echo-caller
namespace TemporalioDocs.Nexus.FeatureGuide.Caller;

using Temporalio.Workflows;

[Workflow]
public class EchoCallerWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync(string message)
    {
        var output = await Workflow.CreateNexusWorkflowClient<IHelloService>(IHelloService.EndpointName).
            ExecuteNexusOperationAsync(svc => svc.Echo(new(message)));
        return output.Message;
    }
}
// @@@SNIPEND
