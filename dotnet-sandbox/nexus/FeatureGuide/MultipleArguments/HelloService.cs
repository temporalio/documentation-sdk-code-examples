namespace TemporalioDocs.Nexus.FeatureGuide.MultipleArguments;

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
        // @@@SNIPSTART dotnet-nexus-feature-guide-multiple-arguments
        client.StartWorkflowAsync(
            (HelloHandlerWorkflow wf) => wf.RunAsync(input.Language, input.Name),
            new() { Id = $"hello-{input.Name}-{input.Language}" });
        // @@@SNIPEND
}
