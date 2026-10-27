namespace TemporalioDocs.Workflows.WorkflowStreams;

using Temporalio.Extensions.WorkflowStreams;
using Temporalio.Workflows;

[Workflow]
public class FixedDelayCloseWorkflow
{
    private readonly WorkflowStreamTopicHandle<StatusEvent> status;

    [WorkflowInit]
    public FixedDelayCloseWorkflow(OrderInput input) =>
        status = new WorkflowStream(input.StreamState).GetTopic<StatusEvent>("status");

    [WorkflowRun]
    public async Task<string> RunAsync(OrderInput input)
    {
        var result = input.OrderId;
        // @@@SNIPSTART dotnet-workflow-streams-close-fixed-delay
        status.Publish(new StatusEvent("completed", 100));
        await Workflow.DelayAsync(TimeSpan.FromSeconds(30));
        return result;
        // @@@SNIPEND
    }
}

[Workflow]
public class AcknowledgedCloseWorkflow
{
    private readonly WorkflowStreamTopicHandle<StatusEvent> status;

    [WorkflowInit]
    public AcknowledgedCloseWorkflow(OrderInput input) =>
        status = new WorkflowStream(input.StreamState).GetTopic<StatusEvent>("status");

    // @@@SNIPSTART dotnet-workflow-streams-close-ack-handler
    private bool subscriberAcknowledged;

    [WorkflowSignal]
    public Task AcknowledgeStreamAsync()
    {
        subscriberAcknowledged = true;
        return Task.CompletedTask;
    }
    // @@@SNIPEND

    [WorkflowRun]
    public async Task<string> RunAsync(OrderInput input)
    {
        var result = input.OrderId;
        status.Publish(new StatusEvent("completed", 100));
        // @@@SNIPSTART dotnet-workflow-streams-close-ack-wait
        await Workflow.WaitConditionAsync(
            () => subscriberAcknowledged,
            TimeSpan.FromSeconds(30));
        return result;
        // @@@SNIPEND
    }
}
