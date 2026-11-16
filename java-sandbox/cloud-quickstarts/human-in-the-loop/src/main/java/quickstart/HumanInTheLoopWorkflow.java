package quickstart;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import quickstart.Models.ApprovalDecision;
import quickstart.Models.WorkflowInput;

/**
 * An AI agent that asks a human to approve risky actions.
 *
 * <p>An LLM proposes an action for the user's request. Safe actions run right away. Risky
 * actions wait for an approval decision delivered by a Signal, with a durable timeout in case no
 * one responds.
 */
@WorkflowInterface
public interface HumanInTheLoopWorkflow {
    /** Returns a sentence describing whether the action ran, was rejected, or timed out. */
    @WorkflowMethod
    String run(WorkflowInput input);

    /** Delivers a reviewer's approve or reject decision. */
    @SignalMethod(name = HumanInTheLoop.APPROVAL_SIGNAL)
    void approvalDecision(ApprovalDecision decision);
}
