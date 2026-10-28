// @@@SNIPSTART dotnet-enriching-ui-timer-summary
namespace TemporalioDocs.Platform.EnrichingUi.TimerSummary;

using Temporalio.Workflows;

[Workflow]
public class YourWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync(string input)
    {
        // Create a timer with a summary
        await Workflow.DelayWithOptionsAsync(new DelayOptions(TimeSpan.FromMinutes(5))
        {
            Summary = "Waiting for payment confirmation"
        });

        return "Timer completed";
    }
}
// @@@SNIPEND
