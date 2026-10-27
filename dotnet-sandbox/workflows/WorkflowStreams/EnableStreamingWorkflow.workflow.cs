namespace TemporalioDocs.Workflows.WorkflowStreams.EnableStreaming;

// @@@SNIPSTART dotnet-workflow-streams-enable
using Temporalio.Extensions.WorkflowStreams;
using Temporalio.Workflows;

public record OrderInput(
    string OrderId,
    WorkflowStreamState? StreamState = null);

[Workflow]
public class OrderWorkflow
{
    private readonly WorkflowStream stream;

    [WorkflowInit]
    public OrderWorkflow(OrderInput input) => stream = new(input.StreamState);

    [WorkflowRun]
    public async Task RunAsync(OrderInput input)
    {
        // ... rest of the Workflow
    }
}
// @@@SNIPEND
