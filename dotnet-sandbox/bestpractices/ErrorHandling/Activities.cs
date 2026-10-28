namespace TemporalioDocs.BestPractices.ErrorHandling;

using Temporalio.Activities;
using Temporalio.Exceptions;

public class Activities
{
    [Activity]
    public Task ValidateCreditCardAsync(string creditCardNumber)
    {
        if (creditCardNumber.Length != 16)
        {
            throw new ApplicationFailureException(
                "Credit card number must be 16 digits", nonRetryable: true);
        }
        return Task.CompletedTask;
    }
}
