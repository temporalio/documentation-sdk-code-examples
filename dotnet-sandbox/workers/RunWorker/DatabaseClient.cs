namespace TemporalioDocs.Workers.RunWorker;

// Stands in for a real database client shared across Activity invocations.
public class DatabaseClient
{
    public Task<string> GetCustomerNameAsync(string customerId) =>
        Task.FromResult($"Customer {customerId}");
}
