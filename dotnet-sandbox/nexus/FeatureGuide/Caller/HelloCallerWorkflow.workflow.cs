// @@@SNIPSTART dotnet-nexus-feature-guide-hello-caller
namespace TemporalioDocs.Nexus.FeatureGuide.Caller;

using Temporalio.Workflows;

[Workflow]
public class HelloCallerWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync(string name, IHelloService.HelloLanguage language)
    {
        var output = await Workflow.CreateNexusWorkflowClient<IHelloService>(IHelloService.EndpointName).
            ExecuteNexusOperationAsync(svc => svc.SayHello(new(name, language)));
        return output.Message;
    }
}
// @@@SNIPEND
