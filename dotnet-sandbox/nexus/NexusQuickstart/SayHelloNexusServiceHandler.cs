// @@@SNIPSTART dotnet-nexus-quickstart-handler
namespace MyNamespace;

using NexusRpc.Handlers;
using Temporalio.Nexus;

[NexusServiceHandler(typeof(ISayHelloNexusService))]
public class SayHelloNexusServiceHandler
{
    [NexusOperationHandler]
    public IOperationHandler<ISayHelloNexusService.MyInput, string> SayHello() =>
        WorkflowRunOperationHandler.FromHandleFactory(
            (WorkflowRunOperationContext context, ISayHelloNexusService.MyInput input) =>
                context.StartWorkflowAsync(
                    (SayHelloWorkflow wf) => wf.RunAsync(input.Name),
                    new() { Id = context.HandlerContext.RequestId }));
}
// @@@SNIPEND
