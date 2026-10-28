namespace TemporalioDocs.Platform.Observability;

using Temporalio.Client;
using Temporalio.Common;

public static class Visibility
{
    public static async Task ListWorkflowsAsync(ITemporalClient client)
    {
        // @@@SNIPSTART dotnet-observability-list-workflows
        await foreach (var wf in client.ListWorkflowsAsync("WorkflowType='GreetingWorkflow'"))
        {
            Console.WriteLine("Workflow: {0}", wf.Id);
        }
        // @@@SNIPEND
    }

    public static async Task<WorkflowHandle> StartWithSearchAttributesAsync(ITemporalClient client)
    {
        // @@@SNIPSTART dotnet-observability-set-search-attributes
        // This only needs to be created once, so it is common to make it a static readonly even though we
        // create inline here for demonstration
        var myKeywordAttributeKey = SearchAttributeKey.CreateKeyword("MyKeywordAttribute");

        // Start workflow with the search attribute collection
        var handle = await client.StartWorkflowAsync(
            (MyWorkflow wf) => wf.RunAsync(),
            new(id: "my-workflow-id", taskQueue: "my-task-queue")
            {
                TypedSearchAttributes = new SearchAttributeCollection.Builder().
                    Set(myKeywordAttributeKey, "SomeKeywordValue").
                    ToSearchAttributeCollection(),
            });
        // @@@SNIPEND
        return handle;
    }
}
