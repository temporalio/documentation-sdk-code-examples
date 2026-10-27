namespace TemporalioDocs.Workflows.MessagePassing;

using Temporalio.Activities;
using Temporalio.Exceptions;
using Temporalio.Workflows;

public record ShoppingCartItem(string Sku, int Quantity);

public static class ShoppingCartActivities
{
    [Activity]
    public static async Task<decimal?> GetPriceAsync(ShoppingCartItem item)
    {
        await Task.Delay(100);
        return item.Sku == "sku-123" ? 5.99M * item.Quantity : null;
    }
}

[Workflow]
public class ShoppingCartWorkflow
{
    private readonly List<decimal> prices = new();
    private bool orderSubmitted;

    [WorkflowRun]
    public async Task<decimal> RunAsync()
    {
        await Workflow.WaitConditionAsync(() => Workflow.AllHandlersFinished && orderSubmitted);
        return Total;
    }

    [WorkflowQuery]
    public decimal Total => prices.Sum();

    [WorkflowUpdate]
    public async Task<decimal> AddItemAsync(ShoppingCartItem item)
    {
        var maybePrice = await Workflow.ExecuteActivityAsync(
            () => ShoppingCartActivities.GetPriceAsync(item),
            new() { ScheduleToCloseTimeout = TimeSpan.FromMinutes(5) });
        var price = maybePrice ??
            throw new ApplicationFailureException($"Item unavailable: {item}", "ItemUnavailable");
        prices.Add(price);
        return Total;
    }

    [WorkflowSignal]
    public async Task CheckoutAsync() => orderSubmitted = true;
}
