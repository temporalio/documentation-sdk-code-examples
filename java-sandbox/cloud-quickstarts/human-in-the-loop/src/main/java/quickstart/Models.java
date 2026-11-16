package quickstart;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The records passed between the Workflow, Activities, and clients.
 *
 * <p>The JSON property names are snake_case so Signal input from the Temporal CLI or UI, for
 * example {@code {"approved": true}}, matches the other language versions of this sample.
 */
public final class Models {

    /** Input for {@link HumanInTheLoopWorkflow}. */
    public record WorkflowInput(
            @JsonProperty("user_request") String userRequest,
            @JsonProperty("approval_timeout_seconds") int approvalTimeoutSeconds) {}

    /** Input for {@link AgentActivities#proposeAction}. */
    public record OpenAIResponsesRequest(
            @JsonProperty("model") String model,
            @JsonProperty("instructions") String instructions,
            @JsonProperty("input") String input) {}

    /** Action the LLM proposes in response to the user request. */
    public record ProposedAction(
            @JsonProperty("action_type") String actionType,
            @JsonProperty("description") String description,
            @JsonProperty("reasoning") String reasoning,
            @JsonProperty("risky_action") boolean riskyAction) {}

    /** Request sent to a human reviewer. */
    public record ApprovalRequest(
            @JsonProperty("request_id") String requestId,
            @JsonProperty("proposed_action") ProposedAction proposedAction,
            @JsonProperty("context") String context,
            @JsonProperty("requested_at") String requestedAt) {}

    /**
     * Decision a human reviewer sends back with a Signal. Leave out {@code requestId} (null) to
     * apply the decision to whichever request is pending.
     */
    public record ApprovalDecision(
            @JsonProperty("approved") boolean approved,
            @JsonProperty("request_id") String requestId,
            @JsonProperty("reviewer_notes") String reviewerNotes,
            @JsonProperty("decided_at") String decidedAt) {}

    private Models() {}
}
