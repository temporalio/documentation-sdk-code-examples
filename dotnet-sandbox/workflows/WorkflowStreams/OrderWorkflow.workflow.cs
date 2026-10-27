namespace TemporalioDocs.Workflows.WorkflowStreams;

using Temporalio.Extensions.WorkflowStreams;
using Temporalio.Workflows;

public record OrderInput(
    string OrderId,
    WorkflowStreamState? StreamState = null);

// @@@SNIPSTART dotnet-workflow-streams-publish-workflow
public record StatusEvent(string State, int Progress = 0, string? Detail = null);

[Workflow]
public partial class OrderWorkflow
{
    private readonly WorkflowStream stream;
    private readonly WorkflowStreamTopicHandle<StatusEvent> status;

    [WorkflowInit]
    public OrderWorkflow(OrderInput input)
    {
        stream = new(input.StreamState);
        status = stream.GetTopic<StatusEvent>("status");
    }

    [WorkflowRun]
    public async Task RunAsync(OrderInput input)
    {
        status.Publish(new StatusEvent("validating", Detail: "checking inventory"));
        await ValidateOrderAsync(input.OrderId);

        status.Publish(new StatusEvent("charging", 33, "authorizing payment"));
        await ChargePaymentAsync(input.OrderId);

        status.Publish(new StatusEvent("shipping", 66, "dispatching to warehouse"));
        await DispatchOrderAsync(input.OrderId);

        status.Publish(new StatusEvent("completed", 100));
        await Workflow.DelayAsync(TimeSpan.FromSeconds(30));
    }
}
// @@@SNIPEND
