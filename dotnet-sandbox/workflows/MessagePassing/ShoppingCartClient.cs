namespace TemporalioDocs.Workflows.MessagePassing;

using Temporalio.Client;
using Temporalio.Exceptions;

public record AddCartItemResult(WorkflowHandle<ShoppingCartWorkflow, decimal> WorkflowHandle, decimal? Subtotal);

public class ShoppingCartClient(ITemporalClient client)
{
    private const string TaskQueue = "update-with-start-lazy-init";

    // @@@SNIPSTART dotnet-message-passing-update-with-start
    async Task<AddCartItemResult> AddCartItemAsync(string sessionId, ShoppingCartItem item)
    {
        // Issue an update-with-start that will create the workflow if it does not
        // exist before attempting the update

        // Create the start operation
        var startOperation = WithStartWorkflowOperation.Create(
            (ShoppingCartWorkflow wf) => wf.RunAsync(),
            new(id: $"cart-{sessionId}", taskQueue: TaskQueue)
            {
                IdConflictPolicy = Temporalio.Api.Enums.V1.WorkflowIdConflictPolicy.UseExisting,
            });

        // Issue the update-with-start, swallowing item-unavailable failure
        decimal? subtotal;
        try
        {
            subtotal = await client.ExecuteUpdateWithStartWorkflowAsync(
                (ShoppingCartWorkflow wf) => wf.AddItemAsync(item),
                new(startOperation));
        }
        catch (WorkflowUpdateFailedException e) when (
            e.InnerException is ApplicationFailureException appErr && appErr.ErrorType == "ItemUnavailable")
        {
            // Set subtotal to null if item was not found
            subtotal = null;
        }

        return new(await startOperation.GetHandleAsync(), subtotal);
    }
    // @@@SNIPEND

    public Task<AddCartItemResult> AddItemAsync(string sessionId, ShoppingCartItem item) =>
        AddCartItemAsync(sessionId, item);
}
