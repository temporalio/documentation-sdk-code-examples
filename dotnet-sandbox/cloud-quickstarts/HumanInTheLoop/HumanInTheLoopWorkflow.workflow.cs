using Microsoft.Extensions.Logging;
using Temporalio.Workflows;

namespace TemporalioDocs.CloudQuickstarts.HumanInTheLoop;

/// <summary>
/// An AI agent that asks a human to approve risky actions.
/// </summary>
/// <remarks>
/// An LLM proposes an action for the user's request. Safe actions run right away. Risky
/// actions wait for an approval decision delivered by a Signal, with a durable timeout in
/// case no one responds.
/// </remarks>
[Workflow]
public class HumanInTheLoopWorkflow
{
    /// <summary>Tells the LLM how to analyze a request and assess its risk.</summary>
    public const string SystemInstructions = """
        You are an AI assistant that analyzes user requests and proposes actions.
        For each request, you should:
        1. Determine what action needs to be taken
        2. Provide a clear description of the action
        3. Explain your reasoning for why this action addresses the request
        4. Assess whether this is a risky action that requires human approval

        Consider an action risky if it:
        - Could cause data loss or corruption
        - Could affect production systems or critical infrastructure
        - Could have financial or legal implications
        - Could impact user experience or system availability
        - Involves deleting, modifying, or overwriting important data
        - Could expose sensitive information or security vulnerabilities

        Be thorough and clear in your analysis.

        Respond with a JSON object in this structure:

        {
          "action_type": "A short name for the action (e.g., \"delete_test_data\")",
          "description": "A clear description of what the action will do",
          "reasoning": "Your explanation for why this action addresses the request",
          "risky_action": true or false indicating whether this action is considered risky
        }
        """;

    // The Signal handler writes these; the wait condition reads them.
    private ApprovalDecision? currentDecision;
    private string? pendingRequestId;

    [WorkflowRun]
    public async Task<string> RunAsync(WorkflowInput input)
    {
        var proposed = await Workflow.ExecuteActivityAsync(
            () => AgentActivities.ProposeActionAsync(new OpenAIResponsesRequest(
                Model: "gpt-4o-mini",
                Instructions: SystemInstructions,
                Input: input.UserRequest)),
            new() { StartToCloseTimeout = TimeSpan.FromSeconds(30) });
        Workflow.Logger.LogInformation(
            "AI proposed action: {ActionType} (risky: {Risky})", proposed.ActionType, proposed.RiskyAction);

        if (!proposed.RiskyAction)
        {
            return $"Action completed successfully (auto-approved): {await ExecuteAsync(proposed)}";
        }

        var decision = await RequestApprovalAsync(proposed, input.UserRequest, input.ApprovalTimeoutSeconds);
        if (decision is null)
        {
            return $"Action cancelled: approval request timed out after {input.ApprovalTimeoutSeconds} seconds";
        }
        if (!decision.Approved)
        {
            return $"Action rejected. Reviewer notes: {decision.ReviewerNotes ?? "None provided"}";
        }
        return $"Action completed successfully: {await ExecuteAsync(proposed)}";
    }

    [WorkflowSignal(Shared.ApprovalSignal)]
    public Task ApprovalDecisionAsync(ApprovalDecision decision)
    {
        if (pendingRequestId is null)
        {
            Workflow.Logger.LogWarning("Ignoring decision: no approval request is pending");
        }
        // A decision without a request_id applies to the pending request. One with a
        // request_id must match it, so a stale decision is ignored.
        else if (decision.RequestId is null || decision.RequestId == pendingRequestId)
        {
            currentDecision = decision;
        }
        else
        {
            Workflow.Logger.LogWarning(
                "Ignoring decision for request {RequestId}; expected {Expected}",
                decision.RequestId,
                pendingRequestId);
        }
        return Task.CompletedTask;
    }

    private static Task<string> ExecuteAsync(ProposedAction action) =>
        Workflow.ExecuteActivityAsync(
            () => AgentActivities.ExecuteAction(action),
            new() { StartToCloseTimeout = TimeSpan.FromSeconds(60) });

    /// <summary>
    /// Notifies a reviewer and waits for a decision. Returns null if the timeout fires
    /// first.
    /// </summary>
    private async Task<ApprovalDecision?> RequestApprovalAsync(
        ProposedAction action, string userRequest, int timeoutSeconds)
    {
        currentDecision = null;
        // Workflow.NewGuid() is deterministic, so it's safe on replay.
        pendingRequestId = Workflow.NewGuid().ToString();

        await Workflow.ExecuteActivityAsync(
            () => AgentActivities.NotifyApprovalNeeded(new ApprovalRequest(
                RequestId: pendingRequestId,
                ProposedAction: action,
                Context: userRequest,
                RequestedAt: Workflow.UtcNow.ToString("O"))),
            new() { StartToCloseTimeout = TimeSpan.FromSeconds(10) });

        // A durable timer: the Workflow uses no compute while it waits, and the timeout
        // survives Worker restarts.
        var decided = await Workflow.WaitConditionAsync(
            () => currentDecision is not null, TimeSpan.FromSeconds(timeoutSeconds));
        return decided ? currentDecision : null;
    }
}
