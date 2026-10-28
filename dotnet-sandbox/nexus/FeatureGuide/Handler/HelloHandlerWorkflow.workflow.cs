namespace TemporalioDocs.Nexus.FeatureGuide.Handler;

using Temporalio.Workflows;

[Workflow]
public class HelloHandlerWorkflow
{
    [WorkflowRun]
    public async Task<IHelloService.HelloOutput> RunAsync(IHelloService.HelloInput input) =>
        input.Language switch
        {
            IHelloService.HelloLanguage.En => new($"Hello {input.Name} 👋"),
            IHelloService.HelloLanguage.Fr => new($"Bonjour {input.Name} 👋"),
            IHelloService.HelloLanguage.De => new($"Hallo {input.Name} 👋"),
            IHelloService.HelloLanguage.Es => new($"¡Hola! {input.Name} 👋"),
            IHelloService.HelloLanguage.Tr => new($"Merhaba {input.Name} 👋"),
            _ => throw new ArgumentException($"Unsupported language: {input.Language}"),
        };
}
