package quickstart;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponseCreateParams;
import io.temporal.activity.Activity;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickstart.Models.ApprovalRequest;
import quickstart.Models.OpenAIResponsesRequest;
import quickstart.Models.ProposedAction;

public class AgentActivitiesImpl implements AgentActivities {
    private static final Logger log = LoggerFactory.getLogger(AgentActivitiesImpl.class);

    /**
     * A fixed, risky action used when no OpenAI API key is set, so the sample runs end to end
     * (including the approval step) without an LLM.
     */
    public static ProposedAction mockProposedAction(String userRequest) {
        return new ProposedAction(
                "delete_test_data",
                "Mock action for the request: " + userRequest,
                "OPENAI_API_KEY is not set, so this action is mocked instead of LLM-generated.",
                true);
    }

    @Override
    public ProposedAction proposeAction(OpenAIResponsesRequest request) {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn(
                    "OPENAI_API_KEY is not set; returning a mock proposed action instead of calling OpenAI.");
            return mockProposedAction(request.input());
        }

        // Let Temporal own retries: disable the OpenAI client's built-in retry logic.
        OpenAIClient client =
                OpenAIOkHttpClient.builder()
                        .apiKey(apiKey)
                        .maxRetries(0)
                        .timeout(Duration.ofSeconds(30))
                        .build();

        // Structured outputs: the API constrains the response to ProposedAction's JSON
        // schema, and the SDK parses it into the record.
        StructuredResponseCreateParams<ProposedAction> params =
                ResponseCreateParams.builder()
                        .model(request.model())
                        .instructions(request.instructions())
                        .input(request.input())
                        .text(ProposedAction.class)
                        .build();

        // Parse here, not in the Workflow: if the output is unusable, this Activity attempt
        // fails and Temporal retries the LLM call. Throwing in Workflow code would instead
        // fail the Workflow Task and replay the same bad result.
        return client.responses().create(params).output().stream()
                .flatMap(item -> item.message().stream())
                .flatMap(message -> message.content().stream())
                .flatMap(content -> content.outputText().stream())
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("LLM returned no parsable ProposedAction"));
    }

    /**
     * This sample prints to the Worker's terminal. A real system might send an email, post to
     * Slack, or update an approval queue UI instead.
     */
    @Override
    public void notifyApprovalNeeded(ApprovalRequest request) {
        String workflowId = Activity.getExecutionContext().getInfo().getWorkflowId();
        log.info("Approval needed for request: {}", request.requestId());

        String line = "=".repeat(60);
        System.out.printf(
                """

                %1$s
                APPROVAL REQUIRED
                %1$s
                Workflow ID: %2$s
                Request ID:  %3$s
                Action:      %4$s
                Description: %5$s
                Reasoning:   %6$s

                To approve or reject, run:
                  mvn -q compile exec:java@send-approval -Dexec.args="approve 'Looks good'"
                  mvn -q compile exec:java@send-approval -Dexec.args="reject 'Too risky'"

                Or with the Temporal CLI:
                  temporal workflow signal --workflow-id %2$s --name %7$s --input '{"approved": true}'
                %1$s

                """,
                line,
                workflowId,
                request.requestId(),
                request.proposedAction().actionType(),
                request.proposedAction().description(),
                request.proposedAction().reasoning(),
                HumanInTheLoop.APPROVAL_SIGNAL);
        System.out.flush();
    }

    /**
     * This sample only logs the action. A real system would call an external API, change a
     * database, or invoke a tool the LLM selected.
     */
    @Override
    public String executeAction(ProposedAction action) {
        log.info("Executing action: {}", action.actionType());
        return "Successfully executed: " + action.actionType();
    }
}
