namespace TemporalioDocs.Nexus.StandaloneOperations;

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
            new() { Id = $"hello-{input.Name}" });
}
