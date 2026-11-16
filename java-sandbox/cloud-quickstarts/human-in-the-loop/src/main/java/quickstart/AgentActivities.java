package quickstart;

import io.temporal.activity.ActivityInterface;
import quickstart.Models.ApprovalRequest;
import quickstart.Models.OpenAIResponsesRequest;
import quickstart.Models.ProposedAction;

/** Activities for the human-in-the-loop agent. */
@ActivityInterface
public interface AgentActivities {
    /**
     * Asks the LLM for a proposed action. If OPENAI_API_KEY isn't set, returns a mocked proposed
     * action instead.
     */
    ProposedAction proposeAction(OpenAIResponsesRequest request);

    /** Tells a human that an action is waiting for approval. */
    void notifyApprovalNeeded(ApprovalRequest request);

    /** Carries out the approved action. */
    String executeAction(ProposedAction action);
}
