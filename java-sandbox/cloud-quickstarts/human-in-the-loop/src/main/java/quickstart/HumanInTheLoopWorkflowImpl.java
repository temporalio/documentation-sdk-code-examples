package quickstart;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import quickstart.Models.ApprovalDecision;
import quickstart.Models.ApprovalRequest;
import quickstart.Models.OpenAIResponsesRequest;
import quickstart.Models.ProposedAction;
import quickstart.Models.WorkflowInput;

public class HumanInTheLoopWorkflowImpl implements HumanInTheLoopWorkflow {
    private static final Logger log = Workflow.getLogger(HumanInTheLoopWorkflowImpl.class);

    /** Tells the LLM how to analyze a request and assess its risk. */
    public static final String SYSTEM_INSTRUCTIONS =
            """
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
              "action_type": "A short name for the action (e.g., \\"delete_test_data\\")",
              "description": "A clear description of what the action will do",
              "reasoning": "Your explanation for why this action addresses the request",
              "risky_action": true or false indicating whether this action is considered risky
            }
            """;

    private final AgentActivities proposeActivities =
            Workflow.newActivityStub(
                    AgentActivities.class,
                    ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(30)).build());

    private final AgentActivities notifyActivities =
            Workflow.newActivityStub(
                    AgentActivities.class,
                    ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

    private final AgentActivities executeActivities =
            Workflow.newActivityStub(
                    AgentActivities.class,
                    ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(60)).build());

    // The Signal handler writes these; the wait condition reads them.
    private ApprovalDecision currentDecision;
    private String pendingRequestId;

    @Override
    public String run(WorkflowInput input) {
        ProposedAction proposed =
                proposeActivities.proposeAction(
                        new OpenAIResponsesRequest("gpt-4o-mini", SYSTEM_INSTRUCTIONS, input.userRequest()));
        log.info(
                "AI proposed action: {} (risky: {})", proposed.actionType(), proposed.riskyAction());

        if (!proposed.riskyAction()) {
            return "Action completed successfully (auto-approved): "
                    + executeActivities.executeAction(proposed);
        }

        ApprovalDecision decision =
                requestApproval(proposed, input.userRequest(), input.approvalTimeoutSeconds());
        if (decision == null) {
            return "Action cancelled: approval request timed out after "
                    + input.approvalTimeoutSeconds()
                    + " seconds";
        }
        if (!decision.approved()) {
            String notes = decision.reviewerNotes() != null ? decision.reviewerNotes() : "None provided";
            return "Action rejected. Reviewer notes: " + notes;
        }
        return "Action completed successfully: " + executeActivities.executeAction(proposed);
    }

    @Override
    public void approvalDecision(ApprovalDecision decision) {
        if (pendingRequestId == null) {
            log.warn("Ignoring decision: no approval request is pending");
        }
        // A decision without a request_id applies to the pending request. One with a
        // request_id must match it, so a stale decision is ignored.
        else if (decision.requestId() == null
                || Objects.equals(decision.requestId(), pendingRequestId)) {
            currentDecision = decision;
        } else {
            log.warn(
                    "Ignoring decision for request {}; expected {}",
                    decision.requestId(),
                    pendingRequestId);
        }
    }

    /** Notifies a reviewer and waits for a decision. Returns null if the timeout fires first. */
    private ApprovalDecision requestApproval(
            ProposedAction action, String userRequest, int timeoutSeconds) {
        currentDecision = null;
        // Workflow.randomUUID() is deterministic, so it's safe on replay.
        pendingRequestId = Workflow.randomUUID().toString();

        notifyActivities.notifyApprovalNeeded(
                new ApprovalRequest(
                        pendingRequestId,
                        action,
                        userRequest,
                        Instant.ofEpochMilli(Workflow.currentTimeMillis()).toString()));

        // A durable timer: the Workflow uses no compute while it waits, and the timeout
        // survives Worker restarts.
        boolean decided =
                Workflow.await(Duration.ofSeconds(timeoutSeconds), () -> currentDecision != null);
        return decided ? currentDecision : null;
    }
}
