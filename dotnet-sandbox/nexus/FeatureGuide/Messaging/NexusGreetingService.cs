namespace TemporalioDocs.Nexus.FeatureGuide.Messaging;

using NexusRpc.Handlers;
using Temporalio.Nexus;

[NexusServiceHandler(typeof(INexusGreetingService))]
public class NexusGreetingService
{
    // @@@SNIPSTART dotnet-nexus-feature-guide-signal
    private static string WorkflowIdForUser(string userId) => $"GreetingWorkflow_for_{userId}";

    [TemporalOperation]
    public async Task<TemporalOperationResult<INexusGreetingService.ApproveOutput>> Approve(
        TemporalOperationStartContext ctx,
        ITemporalNexusClient client,
        INexusGreetingService.ApproveInput input)
    {
        var handle = client.TemporalClient.GetWorkflowHandle<GreetingWorkflow>(
            WorkflowIdForUser(input.UserId));
        await handle.SignalAsync(wf => wf.ApproveAsync(input));
        return TemporalOperationResult<INexusGreetingService.ApproveOutput>.SyncResult(new());
    }
    // @@@SNIPEND
}
