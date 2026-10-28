namespace TemporalioDocs.Nexus.FeatureGuide.MultipleArguments;

using Temporalio.Workflows;

// A handler Workflow that takes the language and name as separate arguments.
[Workflow]
public class HelloHandlerWorkflow
{
    [WorkflowRun]
    public async Task<IHelloService.HelloOutput> RunAsync(IHelloService.HelloLanguage language, string name) =>
        new($"{language}: Hello {name} 👋");
}
