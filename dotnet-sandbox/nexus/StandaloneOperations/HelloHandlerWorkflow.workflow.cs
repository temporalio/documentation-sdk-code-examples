namespace TemporalioDocs.Nexus.StandaloneOperations;

using Temporalio.Workflows;

[Workflow]
public class HelloHandlerWorkflow
{
    [WorkflowRun]
    public Task<IHelloService.HelloOutput> RunAsync(IHelloService.HelloInput input) =>
        Task.FromResult(new IHelloService.HelloOutput($"Hello {input.Name} 👋"));
}
