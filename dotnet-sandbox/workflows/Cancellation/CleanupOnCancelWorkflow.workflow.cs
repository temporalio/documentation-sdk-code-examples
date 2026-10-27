namespace TemporalioDocs.Workflows.Cancellation;

using Microsoft.Extensions.Logging;
using Temporalio.Exceptions;
using Temporalio.Workflows;

[Workflow]
public class CleanupOnCancelWorkflow
{
    // @@@SNIPSTART dotnet-cancellation-handle-in-workflow
    [WorkflowRun]
    public async Task RunAsync()
    {
        try
        {
            // Whether this workflow waits on the activity to handle the cancellation or not is
            // dependent upon the CancellationType option. We leave the default here which sends the
            // cancellation but does not wait on it to be handled.
            await Workflow.ExecuteActivityAsync(
                (MyActivities a) => a.MyNormalActivity(),
                new() { ScheduleToCloseTimeout = TimeSpan.FromMinutes(5) });
        }
        catch (Exception e) when (TemporalException.IsCanceledException(e))
        {
            // The "when" clause above is because we only want to apply the logic to cancellation, but
            // this kind of cleanup could be done on any/all exceptions too.
            Workflow.Logger.LogError(e, "Cancellation occurred, performing cleanup");

            // Call cleanup activity. If this throws, it will swallow the original exception which we
            // are ok with here. This could be changed to just log a failure and let the original
            // cancellation continue.
            // The default token on Workflow.CancellationToken is now marked
            // cancelled, so we pass a different one. We use CancellationToken.None here because the
            // cleanup activity itself doesn't need to be cancellable; if it did (e.g. you want to
            // cancel cleanup from a timeout or another signal), create a new detached
            // CancellationTokenSource and pass its Token instead.
            await Workflow.ExecuteActivityAsync(
                (MyActivities a) => a.MyCancellationCleanupActivity(),
                new()
                {
                    ScheduleToCloseTimeout = TimeSpan.FromMinutes(5),
                    CancellationToken = CancellationToken.None,
                });

            // Rethrow the cancellation
            throw;
        }
    }
    // @@@SNIPEND
}
