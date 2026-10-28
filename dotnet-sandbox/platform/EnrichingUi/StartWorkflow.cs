// @@@SNIPSTART dotnet-enriching-ui-start-workflow
using Temporalio.Client;
using TemporalioDocs.Platform.EnrichingUi.CurrentDetails;

public static class StartWorkflow
{
    public static async Task<WorkflowHandle<YourWorkflow, string>> RunAsync()
    {
        // Create client
        var client = await TemporalClient.ConnectAsync(new("localhost:7233"));

        // Start a Workflow with static summary and details
        var handle = await client.StartWorkflowAsync(
            (YourWorkflow wf) => wf.RunAsync("Workflow input"),
            new WorkflowOptions
            {
                Id = "your-workflow-id",
                TaskQueue = "your-task-queue",
                StaticSummary = "Order processing for customer #12345",
                StaticDetails = "Processing premium order with expedited shipping"
            });
        return handle;
    }
}
// @@@SNIPEND
