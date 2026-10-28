namespace TemporalioDocs.BestPractices.ErrorHandling;

using Temporalio.Activities;
using Temporalio.Exceptions;

public class ApplicationFailureActivities
{
    // @@@SNIPSTART dotnet-error-handling-application-failure
    [Activity]
    public Task<OrderConfirmation> SendBillAsync(Bill bill)
    {
        throw new ApplicationFailureException("Invalid department", errorType: "InvalidDepartmentException");
    }
    // @@@SNIPEND
}
