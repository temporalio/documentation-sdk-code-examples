namespace TemporalioDocs.Workflows.WorkflowStreams;

using Temporalio.Workflows;

public partial class OrderWorkflow
{
    private static Task ValidateOrderAsync(string orderId) =>
        Workflow.DelayAsync(TimeSpan.FromSeconds(1));

    private static Task ChargePaymentAsync(string orderId) =>
        Workflow.DelayAsync(TimeSpan.FromSeconds(1));

    private static Task DispatchOrderAsync(string orderId) =>
        Workflow.DelayAsync(TimeSpan.FromSeconds(1));
}
