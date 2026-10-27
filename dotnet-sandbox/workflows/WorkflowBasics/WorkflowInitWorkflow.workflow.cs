namespace TemporalioDocs.Workflows.WorkflowBasics;

using Temporalio.Workflows;

// @@@SNIPSTART dotnet-workflow-basics-init
[Workflow]
public class WorkflowInitWorkflow
{
    public record Input(string Name);

    private readonly string nameWithTitle;
    private bool titleHasBeenChecked;

    [WorkflowInit]
    public WorkflowInitWorkflow(Input input) =>
        nameWithTitle = $"Knight {input.Name}";

    [WorkflowRun]
    public async Task<string> RunAsync(Input ignored)
    {
        await Workflow.WaitConditionAsync(() => titleHasBeenChecked);
        return $"Hello, {nameWithTitle}";
    }

    [WorkflowUpdate]
    public async Task<bool> CheckTitleValidityAsync()
    {
        // The handler can read nameWithTitle because the constructor set it.
        titleHasBeenChecked = true;
        return nameWithTitle.StartsWith("Knight ", StringComparison.Ordinal);
    }
}
// @@@SNIPEND
