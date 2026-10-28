namespace TemporalioDocs.Workers.RunWorker;

using Temporalio.Activities;

public class MyActivities
{
    private readonly DatabaseClient databaseClient;

    public MyActivities(DatabaseClient databaseClient) => this.databaseClient = databaseClient;

    [Activity]
    public Task<string> GetCustomerNameAsync(string customerId) =>
        databaseClient.GetCustomerNameAsync(customerId);
}
