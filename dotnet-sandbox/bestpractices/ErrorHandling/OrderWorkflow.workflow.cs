namespace TemporalioDocs.BestPractices.ErrorHandling;

using Microsoft.Extensions.Logging;
using Temporalio.Exceptions;
using Temporalio.Workflows;

[Workflow]
public class OrderWorkflow
{
    [WorkflowRun]
    public async Task RunAsync(Order order)
    {
        var logger = Workflow.Logger;
        var options = new ActivityOptions { StartToCloseTimeout = TimeSpan.FromSeconds(10) };

        // @@@SNIPSTART dotnet-error-handling-fail-workflow
        try
        {
            await Workflow.ExecuteActivityAsync(
                (Activities act) => act.ValidateCreditCardAsync(order.Customer.CreditCardNumber),
                options);
        }
        catch (ActivityFailureException err)
        {
            logger.LogError("Unable to process credit card: {Message}", err.Message);
            throw new ApplicationFailureException(message: "Invalid credit card number error");
        }
        // @@@SNIPEND
    }
}
