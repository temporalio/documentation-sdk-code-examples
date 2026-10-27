namespace TemporalioDocs.Workflows.Cancellation;

using Microsoft.Extensions.Logging;
using Temporalio.Activities;

public class MyActivities
{
    [Activity]
    public async Task MyNormalActivity()
    {
        while (true)
        {
            ActivityExecutionContext.Current.Heartbeat();
            await Task.Delay(1000, ActivityExecutionContext.Current.CancellationToken);
        }
    }

    [Activity]
    public void MyCancellationCleanupActivity() =>
        ActivityExecutionContext.Current.Logger.LogInformation("Cleaning up after cancellation");

    // @@@SNIPSTART dotnet-cancellation-activity-heartbeat
    [Activity]
    public async Task MyActivityAsync()
    {
        // This is a naive loop simulating work, but similar heartbeat/cancellation logic applies to
        // other scenarios as well
        while (true)
        {
            // Send heartbeat
            ActivityExecutionContext.Current.Heartbeat();

            // Do some work, passing the cancellation token
            await Task.Delay(1000, ActivityExecutionContext.Current.CancellationToken);
        }
    }
    // @@@SNIPEND
}
