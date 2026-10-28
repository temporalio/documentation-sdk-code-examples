// @@@SNIPSTART dotnet-enriching-ui-current-details
namespace TemporalioDocs.Platform.EnrichingUi.CurrentDetails;

using Microsoft.Extensions.Logging;
using Temporalio.Workflows;

[Workflow]
public class YourWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync(string input)
    {
        // Get the current details
        var currentDetails = Workflow.CurrentDetails;
        Workflow.Logger.LogInformation("Current details: {CurrentDetails}", currentDetails);

        // Set/update the current details
        Workflow.CurrentDetails = "Updated Workflow details with new status";

        return "Workflow completed";
    }
}
// @@@SNIPEND
