namespace TemporalioDocs.Platform.EnrichingUi;

using Temporalio.Client;
using TemporalioDocs.Platform.EnrichingUi.CurrentDetails;

public static class ExecuteWorkflow
{
    public static async Task<string> RunAsync(ITemporalClient client)
    {
        // @@@SNIPSTART dotnet-enriching-ui-execute-workflow
        var result = await client.ExecuteWorkflowAsync(
            (YourWorkflow wf) => wf.RunAsync("Workflow input"),
            new WorkflowOptions
            {
                Id = "your-workflow-id",
                TaskQueue = "your-task-queue",
                StaticSummary = "Order processing for customer #12345",
                StaticDetails = "Processing premium order with expedited shipping"
            });
        // @@@SNIPEND
        return result;
    }
}
