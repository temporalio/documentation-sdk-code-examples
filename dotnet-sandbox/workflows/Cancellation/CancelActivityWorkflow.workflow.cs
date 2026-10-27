namespace TemporalioDocs.Workflows.Cancellation;

using Temporalio.Workflows;

[Workflow]
public class CancelActivityWorkflow
{
    // @@@SNIPSTART dotnet-cancellation-cancel-activity
    [WorkflowRun]
    public async Task RunAsync()
    {
        // Create a source linked to workflow cancellation. A new source could be created instead if we
        // didn't want it associated with workflow cancellation.
        using var cancelActivitySource = CancellationTokenSource.CreateLinkedTokenSource(
            Workflow.CancellationToken);

        // Start the activity. Whether this workflow waits on the activity to handle the cancellation
        // or not is dependent upon the CancellationType option. We leave the default here which sends
        // the cancellation but does not wait on it to be handled.
        var activityTask = Workflow.ExecuteActivityAsync(
            (MyActivities a) => a.MyNormalActivity(),
            new()
            {
                ScheduleToCloseTimeout = TimeSpan.FromMinutes(5),
                CancellationToken = cancelActivitySource.Token,
            });

        // Wait 5 minutes, then cancel it
        await Workflow.DelayAsync(TimeSpan.FromMinutes(5));
        cancelActivitySource.Cancel();

        // Wait on the activity which will throw cancellation which will fail the workflow
        await activityTask;
    }
    // @@@SNIPEND
}
