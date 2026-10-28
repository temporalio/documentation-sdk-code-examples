namespace TemporalioDocs.BestPractices.ErrorHandling;

using Temporalio.Activities;

public class CustomExceptionActivities
{
    // @@@SNIPSTART dotnet-error-handling-custom-exception
    [Serializable]
    public class InvalidDepartmentException : Exception
    {
        public InvalidDepartmentException() : base() { }
        public InvalidDepartmentException(string message) : base(message) { }
        public InvalidDepartmentException(string message, Exception inner) : base(message, inner) { }
    }

    [Activity]
    public Task<OrderConfirmation> SendBillAsync(Bill bill)
    {
        throw new InvalidDepartmentException("Invalid department");
    }
    // @@@SNIPEND
}
