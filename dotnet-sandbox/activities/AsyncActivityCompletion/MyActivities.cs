namespace TemporalioDocs.Activities.AsyncActivityCompletion;

using Temporalio.Activities;

public class MyActivities
{
    private byte[]? capturedToken;

    public byte[]? CapturedToken => capturedToken;

    [Activity]
    public string RequestApproval()
    {
        // @@@SNIPSTART dotnet-async-activity-completion-capture-token
        // Capture token for later completion
        capturedToken = ActivityExecutionContext.Current.Info.TaskToken;

        // Throw special exception that says an activity will be completed somewhere else
        throw new CompleteAsyncException();
        // @@@SNIPEND
    }
}
