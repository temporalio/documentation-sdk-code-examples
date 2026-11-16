using System.Threading.Channels;
using Temporalio.Activities;
using Temporalio.Client;
using Temporalio.Testing;
using Temporalio.Worker;
using TemporalioDocs.CloudQuickstarts.HumanInTheLoop;

namespace TemporalioDocs.CloudQuickstarts.HumanInTheLoop.Tests;

public class HumanInTheLoopWorkflowTests
{
    private static readonly ProposedAction Safe = new(
        "list_files", "List directory contents", "Read-only operation", RiskyAction: false);

    private static readonly ProposedAction Risky = new(
        "delete_data", "Delete production data", "User asked for deletion", RiskyAction: true);

    /// <summary>
    /// Stand-ins for the real Activities, registered under the same names, so the tests
    /// need no API key.
    /// </summary>
    private class MockActivities(ProposedAction action)
    {
        public Channel<ApprovalRequest> Requests { get; } = Channel.CreateUnbounded<ApprovalRequest>();

        [Activity("ProposeAction")]
        public ProposedAction ProposeAction(OpenAIResponsesRequest request) => action;

        [Activity("NotifyApprovalNeeded")]
        public void NotifyApprovalNeeded(ApprovalRequest request) => Requests.Writer.TryWrite(request);

        [Activity("ExecuteAction")]
        public string ExecuteAction(ProposedAction proposed) => $"Executed: {proposed.ActionType}";
    }

    /// <summary>
    /// Runs the Workflow with mocked Activities. If <paramref name="decide"/> is set, sends
    /// it as the reviewer's decision once approval is requested.
    /// </summary>
    private static async Task<string> RunWorkflowAsync(
        ProposedAction action,
        int timeoutSeconds,
        (bool Approved, string Notes)? decide = null,
        bool withRequestId = true)
    {
        // Time skipping makes the approval timeout fire instantly.
        await using var env = await WorkflowEnvironment.StartTimeSkippingAsync();
        var taskQueue = Guid.NewGuid().ToString();
        var mocks = new MockActivities(action);

        using var worker = new TemporalWorker(
            env.Client,
            new TemporalWorkerOptions(taskQueue)
                .AddWorkflow<HumanInTheLoopWorkflow>()
                .AddAllActivities(mocks));

        return await worker.ExecuteAsync(async () =>
        {
            var handle = await env.Client.StartWorkflowAsync(
                (HumanInTheLoopWorkflow wf) => wf.RunAsync(new WorkflowInput("test", timeoutSeconds)),
                new(id: $"human-in-the-loop-{Guid.NewGuid()}", taskQueue: taskQueue)
                {
                    RunTimeout = TimeSpan.FromSeconds(30),
                });

            if (decide is { } d)
            {
                using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
                var request = await mocks.Requests.Reader.ReadAsync(cts.Token);
                var decision = new ApprovalDecision(
                    d.Approved,
                    RequestId: withRequestId ? request.RequestId : null,
                    ReviewerNotes: d.Notes,
                    DecidedAt: "2026-01-01T00:00:00Z");
                await handle.SignalAsync(wf => wf.ApprovalDecisionAsync(decision));
            }
            return await handle.GetResultAsync();
        });
    }

    [Fact]
    public async Task SafeAction_IsAutoApproved()
    {
        var result = await RunWorkflowAsync(Safe, timeoutSeconds: 30);
        Assert.Equal("Action completed successfully (auto-approved): Executed: list_files", result);
    }

    [Fact]
    public async Task RiskyAction_RunsWhenApproved()
    {
        var result = await RunWorkflowAsync(Risky, timeoutSeconds: 300, decide: (true, "Looks good"));
        Assert.Equal("Action completed successfully: Executed: delete_data", result);
    }

    [Fact]
    public async Task DecisionWithoutRequestId_AppliesToPendingRequest()
    {
        var result = await RunWorkflowAsync(
            Risky, timeoutSeconds: 300, decide: (true, "Looks good"), withRequestId: false);
        Assert.Equal("Action completed successfully: Executed: delete_data", result);
    }

    [Fact]
    public async Task RiskyAction_IsRejected()
    {
        var result = await RunWorkflowAsync(Risky, timeoutSeconds: 300, decide: (false, "Too risky"));
        Assert.Equal("Action rejected. Reviewer notes: Too risky", result);
    }

    [Fact]
    public async Task RiskyAction_TimesOutWithoutADecision()
    {
        var result = await RunWorkflowAsync(Risky, timeoutSeconds: 1);
        Assert.Contains("timed out after 1 seconds", result);
    }

    [Fact]
    public async Task ProposeAction_ReturnsRiskyMockWithoutApiKey()
    {
        var saved = Environment.GetEnvironmentVariable("OPENAI_API_KEY");
        Environment.SetEnvironmentVariable("OPENAI_API_KEY", null);
        try
        {
            var action = await new ActivityEnvironment().RunAsync(() =>
                AgentActivities.ProposeActionAsync(new OpenAIResponsesRequest("gpt-4o-mini", string.Empty, "test")));
            Assert.True(action.RiskyAction);
            Assert.Equal("delete_test_data", action.ActionType);
        }
        finally
        {
            Environment.SetEnvironmentVariable("OPENAI_API_KEY", saved);
        }
    }
}
