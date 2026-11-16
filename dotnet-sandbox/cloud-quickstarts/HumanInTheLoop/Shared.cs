using System.Text.Json.Serialization;

namespace TemporalioDocs.CloudQuickstarts.HumanInTheLoop;

/// <summary>Shared constants for the Worker and clients.</summary>
public static class Shared
{
    /// <summary>Keep the Worker and clients pointed at the same value.</summary>
    public static readonly string TaskQueue = EnvOr("TEMPORAL_TASK_QUEUE", "quickstart-human-in-the-loop");

    /// <summary>
    /// A fixed Workflow ID, so approval commands can be written as fixed text. Only one
    /// Workflow with this ID can run at a time.
    /// </summary>
    public static readonly string WorkflowId = EnvOr("HUMAN_IN_THE_LOOP_WORKFLOW_ID", "human-in-the-loop");

    /// <summary>Name of the Signal that carries an <see cref="ApprovalDecision"/>.</summary>
    public const string ApprovalSignal = "approval_decision";

    private static string EnvOr(string key, string fallback) =>
        Environment.GetEnvironmentVariable(key) is { Length: > 0 } value ? value : fallback;
}

// The JSON property names are snake_case so Signal input from the Temporal CLI or UI,
// for example {"approved": true}, matches the other language versions of this sample.

/// <summary>Input for <see cref="HumanInTheLoopWorkflow"/>.</summary>
public record WorkflowInput(
    [property: JsonPropertyName("user_request")] string UserRequest,
    [property: JsonPropertyName("approval_timeout_seconds")] int ApprovalTimeoutSeconds = 300);

/// <summary>Action the LLM proposes in response to the user request.</summary>
public record ProposedAction(
    [property: JsonPropertyName("action_type")] string ActionType,
    [property: JsonPropertyName("description")] string Description,
    [property: JsonPropertyName("reasoning")] string Reasoning,
    [property: JsonPropertyName("risky_action")] bool RiskyAction);

/// <summary>Request sent to a human reviewer.</summary>
public record ApprovalRequest(
    [property: JsonPropertyName("request_id")] string RequestId,
    [property: JsonPropertyName("proposed_action")] ProposedAction ProposedAction,
    [property: JsonPropertyName("context")] string Context,
    [property: JsonPropertyName("requested_at")] string RequestedAt);

/// <summary>
/// Decision a human reviewer sends back with a Signal. Leave out RequestId to apply the
/// decision to whichever request is pending.
/// </summary>
public record ApprovalDecision(
    [property: JsonPropertyName("approved")] bool Approved,
    [property: JsonPropertyName("request_id")] string? RequestId = null,
    [property: JsonPropertyName("reviewer_notes")] string? ReviewerNotes = null,
    [property: JsonPropertyName("decided_at")] string? DecidedAt = null);
