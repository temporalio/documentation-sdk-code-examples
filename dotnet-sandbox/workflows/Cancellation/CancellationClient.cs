namespace TemporalioDocs.Workflows.Cancellation;

using Temporalio.Client;

public static class CancellationClient
{
    public static async Task CancelWorkflowAsync(ITemporalClient myClient)
    {
        // @@@SNIPSTART dotnet-cancellation-request
        // Get a workflow handle by its workflow ID. This could be made specific to a run by passing run ID.
        // This could also just be a handle that is returned from StartWorkflowAsync instead.
        var handle = myClient.GetWorkflowHandle("my-workflow-id");

        // Send cancellation. This returns when cancellation is received by the server. Wait on the handle's
        // result to wait for cancellation to be applied.
        await handle.CancelAsync();
        // @@@SNIPEND
    }

    public static async Task TerminateWorkflowAsync(ITemporalClient myClient)
    {
        // @@@SNIPSTART dotnet-cancellation-terminate
        // Get a workflow handle by its workflow ID. This could be made specific to a run by passing run ID.
        // This could also just be a handle that is returned from StartWorkflowAsync instead.
        var handle = myClient.GetWorkflowHandle("my-workflow-id");

        // Terminate
        await handle.TerminateAsync();
        // @@@SNIPEND
    }
}
