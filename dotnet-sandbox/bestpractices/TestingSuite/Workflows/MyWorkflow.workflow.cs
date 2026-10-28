namespace TemporalioDocs.BestPractices.TestingSuite;

using Temporalio.Workflows;

[Workflow]
public class MyWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync(string name)
    {
        await Workflow.DelayAsync(TimeSpan.FromSeconds(1));
        return $"Hello, {name}!";
    }
}
