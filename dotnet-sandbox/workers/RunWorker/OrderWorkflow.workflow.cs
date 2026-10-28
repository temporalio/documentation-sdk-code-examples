namespace TemporalioDocs.Workers.RunWorker;

using Temporalio.Workflows;

[Workflow]
public class OrderWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync(string orderId, string customerId)
    {
        var name = await Workflow.ExecuteActivityAsync(
            (MyActivities a) => a.GetCustomerNameAsync(customerId),
            new() { StartToCloseTimeout = TimeSpan.FromMinutes(1) });
        return $"Order {orderId} placed for {name}";
    }
}
