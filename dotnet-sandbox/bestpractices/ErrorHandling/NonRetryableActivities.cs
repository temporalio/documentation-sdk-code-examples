namespace TemporalioDocs.BestPractices.ErrorHandling;

using Temporalio.Activities;
using Temporalio.Exceptions;

public class NonRetryableActivities
{
    // @@@SNIPSTART dotnet-error-handling-non-retryable
    [Activity]
    public Task<OrderConfirmation> SendBillAsync(Bill bill)
    {
        throw new ApplicationFailureException("Invalid department", nonRetryable: true);
    }
    // @@@SNIPEND
}
