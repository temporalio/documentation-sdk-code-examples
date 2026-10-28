namespace TemporalioDocs.Activities.AsyncActivityCompletion;

using Temporalio.Client;

public static class ExternalCompleter
{
    public static async Task CompleteAsync(ITemporalClient myClient, byte[] capturedToken)
    {
        // @@@SNIPSTART dotnet-async-activity-completion-get-handle
        var handle = myClient.GetAsyncActivityHandle(capturedToken);
        // @@@SNIPEND

        // @@@SNIPSTART dotnet-async-activity-completion-complete
        await handle.CompleteAsync("Completion value.");
        // @@@SNIPEND
    }
}
