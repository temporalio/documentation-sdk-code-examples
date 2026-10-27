namespace TemporalioDocs.Workflows.ContinueAsNew;

using Microsoft.Extensions.Logging;
using Temporalio.Exceptions;
using Temporalio.Workflows;

[Workflow]
public class ClusterManagerWorkflow
{
    public record State
    {
        public bool ClusterStarted { get; set; }

        public bool ClusterShutdown { get; set; }

        public IDictionary<string, string?> Nodes { get; init; } = new Dictionary<string, string?>();
    }

    private readonly int maxHistoryLength;
    private readonly TimeSpan sleepInterval;

    // @@@SNIPSTART dotnet-continue-as-new-input
    public record Input
    {
        public State State { get; init; } = new();

        public bool TestContinueAsNew { get; init; }
    }

    [WorkflowInit]
    public ClusterManagerWorkflow(Input input)
    {
        CurrentState = input.State;
        maxHistoryLength = input.TestContinueAsNew ? 40 : int.MaxValue;
        sleepInterval = TimeSpan.FromSeconds(input.TestContinueAsNew ? 1 : 600);
    }
    // @@@SNIPEND

    [WorkflowQuery]
    public State CurrentState { get; init; }

    [WorkflowRun]
    public async Task RunAsync(Input input)
    {
        await Workflow.WaitConditionAsync(() => CurrentState.ClusterStarted);

        do
        {
            await Workflow.WaitConditionAsync(
                () => CurrentState.ClusterShutdown || ShouldContinueAsNew,
                sleepInterval);

            if (ShouldContinueAsNew)
            {
                Workflow.Logger.LogInformation("Continuing as new");
                // @@@SNIPSTART dotnet-continue-as-new-throw
                throw Workflow.CreateContinueAsNewException((ClusterManagerWorkflow wf) => wf.RunAsync(new()
                {
                    State = CurrentState,
                    TestContinueAsNew = input.TestContinueAsNew,
                }));
                // @@@SNIPEND
            }
        }
        while (!CurrentState.ClusterShutdown);
    }

    [WorkflowSignal]
    public async Task StartClusterAsync()
    {
        CurrentState.ClusterStarted = true;
        foreach (var node in Enumerable.Range(0, 25))
        {
            CurrentState.Nodes[$"{node}"] = null;
        }
    }

    [WorkflowSignal]
    public async Task ShutdownClusterAsync()
    {
        await Workflow.WaitConditionAsync(() => CurrentState.ClusterStarted);
        CurrentState.ClusterShutdown = true;
    }

    [WorkflowUpdate]
    public async Task<List<string>> AllocateNodesToJobAsync(int numNodes, string jobName)
    {
        await Workflow.WaitConditionAsync(() => CurrentState.ClusterStarted);
        if (CurrentState.ClusterShutdown)
        {
            throw new ApplicationFailureException(
                "Cannot allocate nodes to a job, cluster is already shut down");
        }
        var unassignedNodes = CurrentState.Nodes.
            Where(kvp => kvp.Value == null).
            Select(kvp => kvp.Key).
            Take(numNodes).
            ToList();
        if (unassignedNodes.Count < numNodes)
        {
            throw new ApplicationFailureException(
                $"Cannot allocate {numNodes} nodes, have only {unassignedNodes.Count} available");
        }
        foreach (var node in unassignedNodes)
        {
            CurrentState.Nodes[node] = jobName;
        }
        return unassignedNodes;
    }

    // @@@SNIPSTART dotnet-continue-as-new-should-continue
    private bool ShouldContinueAsNew =>
        // Don't continue as new while update running
        Workflow.AllHandlersFinished &&
        // Continue if suggested or, for ease of testing, max history reached
        (Workflow.ContinueAsNewSuggested || Workflow.CurrentHistoryLength > maxHistoryLength);
    // @@@SNIPEND
}
