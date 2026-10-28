// @@@SNIPSTART dotnet-nexus-feature-guide-hello-service
namespace TemporalioDocs.Nexus.FeatureGuide.Handler;

using NexusRpc.Handlers;
using Temporalio.Nexus;

[NexusServiceHandler(typeof(IHelloService))]
public class HelloService
{
    [TemporalOperation]
    public Task<TemporalOperationResult<IHelloService.EchoOutput>> Echo(
        TemporalOperationStartContext ctx,
        ITemporalNexusClient client,
        IHelloService.EchoInput input) =>
        Task.FromResult(TemporalOperationResult<IHelloService.EchoOutput>.SyncResult(
            new(input.Message)));

    [TemporalOperation]
    public Task<TemporalOperationResult<IHelloService.HelloOutput>> SayHello(
        TemporalOperationStartContext ctx,
        ITemporalNexusClient client,
        IHelloService.HelloInput input) =>
        client.StartWorkflowAsync(
            (HelloHandlerWorkflow wf) => wf.RunAsync(input),
            // Workflow IDs should typically be business meaningful IDs and are used to dedupe
            // workflow starts. Task queue defaults to the operation's task queue when omitted.
            new() { Id = $"hello-{input.Name}-{input.Language}" });
}
// @@@SNIPEND
