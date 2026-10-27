namespace TemporalioDocs.Workflows.WorkflowStreams;

// @@@SNIPSTART dotnet-workflow-streams-continue-as-new
using Temporalio.Extensions.WorkflowStreams;
using Temporalio.Workflows;

public record PipelineInput(
    int ItemsProcessed = 0,
    WorkflowStreamState? StreamState = null);

[Workflow]
public partial class PipelineWorkflow
{
    private readonly WorkflowStream stream;

    [WorkflowInit]
    public PipelineWorkflow(PipelineInput input) => stream = new(input.StreamState);

    [WorkflowRun]
    public async Task RunAsync(PipelineInput input)
    {
        var itemsProcessed = input.ItemsProcessed;
        var progress = stream.GetTopic<ProgressEvent>("progress");

        while (true)
        {
            await ProcessNextItemAsync();
            itemsProcessed++;
            progress.Publish(new ProgressEvent($"processed {itemsProcessed} items"));

            if (Workflow.ContinueAsNewSuggested)
            {
                var streamState = await stream.CaptureStateForContinueAsNewAsync();
                throw Workflow.CreateContinueAsNewException(
                    (PipelineWorkflow workflow) => workflow.RunAsync(
                        new(itemsProcessed, streamState)));
            }
        }
    }
}
// @@@SNIPEND

public partial class PipelineWorkflow
{
    private static Task ProcessNextItemAsync() => Workflow.DelayAsync(TimeSpan.FromSeconds(1));
}
