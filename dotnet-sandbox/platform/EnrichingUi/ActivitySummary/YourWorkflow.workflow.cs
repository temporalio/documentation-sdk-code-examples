// @@@SNIPSTART dotnet-enriching-ui-activity-summary
namespace TemporalioDocs.Platform.EnrichingUi.ActivitySummary;

using Temporalio.Workflows;

[Workflow]
public class YourWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync(string input)
    {
        // Execute an activity with a summary
        var result = await Workflow.ExecuteActivityAsync(
            (YourActivities act) => act.YourActivityAsync(input),
            new ActivityOptions
            {
                StartToCloseTimeout = TimeSpan.FromSeconds(10),
                Summary = "Processing user data"
            });

        return result;
    }
}
// @@@SNIPEND
