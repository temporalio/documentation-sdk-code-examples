package quickstart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestActivityEnvironment;
import io.temporal.testing.TestWorkflowEnvironment;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import quickstart.Models.ApprovalDecision;
import quickstart.Models.ApprovalRequest;
import quickstart.Models.OpenAIResponsesRequest;
import quickstart.Models.ProposedAction;
import quickstart.Models.WorkflowInput;

class HumanInTheLoopWorkflowTest {
    private static final ProposedAction SAFE =
            new ProposedAction("list_files", "List directory contents", "Read-only operation", false);
    private static final ProposedAction RISKY =
            new ProposedAction(
                    "delete_data", "Delete production data", "User asked for deletion", true);

    /** Stand-ins for the real Activities, so the tests need no API key. */
    private static class MockActivities implements AgentActivities {
        private final ProposedAction action;
        final BlockingQueue<ApprovalRequest> requests = new LinkedBlockingQueue<>();

        MockActivities(ProposedAction action) {
            this.action = action;
        }

        @Override
        public ProposedAction proposeAction(OpenAIResponsesRequest request) {
            return action;
        }

        @Override
        public void notifyApprovalNeeded(ApprovalRequest request) {
            requests.add(request);
        }

        @Override
        public String executeAction(ProposedAction proposed) {
            return "Executed: " + proposed.actionType();
        }
    }

    private record Decision(boolean approved, String notes) {}

    /**
     * Runs the Workflow with mocked Activities. If {@code decide} is set, sends it as the
     * reviewer's decision once approval is requested.
     */
    private static String runWorkflow(
            ProposedAction action, int timeoutSeconds, Decision decide, boolean withRequestId)
            throws InterruptedException {
        // The test environment skips time, so the approval timeout fires instantly.
        try (TestWorkflowEnvironment env = TestWorkflowEnvironment.newInstance()) {
            String taskQueue = UUID.randomUUID().toString();
            MockActivities mocks = new MockActivities(action);
            var worker = env.newWorker(taskQueue);
            worker.registerWorkflowImplementationTypes(HumanInTheLoopWorkflowImpl.class);
            worker.registerActivitiesImplementations(mocks);
            env.start();

            HumanInTheLoopWorkflow workflow =
                    env.getWorkflowClient()
                            .newWorkflowStub(
                                    HumanInTheLoopWorkflow.class,
                                    WorkflowOptions.newBuilder()
                                            .setWorkflowId("human-in-the-loop-" + UUID.randomUUID())
                                            .setTaskQueue(taskQueue)
                                            .setWorkflowRunTimeout(Duration.ofSeconds(30))
                                            .build());
            WorkflowClient.start(workflow::run, new WorkflowInput("test", timeoutSeconds));

            if (decide != null) {
                ApprovalRequest request = mocks.requests.poll(10, TimeUnit.SECONDS);
                workflow.approvalDecision(
                        new ApprovalDecision(
                                decide.approved(),
                                withRequestId ? request.requestId() : null,
                                decide.notes(),
                                "2026-01-01T00:00:00Z"));
            }
            return WorkflowStub.fromTyped(workflow).getResult(String.class);
        }
    }

    @Test
    void safeActionIsAutoApproved() throws Exception {
        assertEquals(
                "Action completed successfully (auto-approved): Executed: list_files",
                runWorkflow(SAFE, 30, null, true));
    }

    @Test
    void riskyActionRunsWhenApproved() throws Exception {
        assertEquals(
                "Action completed successfully: Executed: delete_data",
                runWorkflow(RISKY, 300, new Decision(true, "Looks good"), true));
    }

    @Test
    void decisionWithoutRequestIdAppliesToPendingRequest() throws Exception {
        assertEquals(
                "Action completed successfully: Executed: delete_data",
                runWorkflow(RISKY, 300, new Decision(true, "Looks good"), false));
    }

    @Test
    void riskyActionIsRejected() throws Exception {
        assertEquals(
                "Action rejected. Reviewer notes: Too risky",
                runWorkflow(RISKY, 300, new Decision(false, "Too risky"), true));
    }

    @Test
    void riskyActionTimesOutWithoutADecision() throws Exception {
        assertTrue(runWorkflow(RISKY, 1, null, true).contains("timed out after 1 seconds"));
    }

    @Test
    void proposeActionReturnsRiskyMockWithoutApiKey() {
        // Runs only when OPENAI_API_KEY is unset, since Java can't clear an env var.
        org.junit.jupiter.api.Assumptions.assumeTrue(
                System.getenv("OPENAI_API_KEY") == null || System.getenv("OPENAI_API_KEY").isEmpty(),
                "OPENAI_API_KEY is set");
        TestActivityEnvironment env = TestActivityEnvironment.newInstance();
        try {
            env.registerActivitiesImplementations(new AgentActivitiesImpl());
            AgentActivities activities = env.newActivityStub(AgentActivities.class);
            ProposedAction action =
                    activities.proposeAction(new OpenAIResponsesRequest("gpt-4o-mini", "", "test"));
            assertTrue(action.riskyAction());
            assertEquals("delete_test_data", action.actionType());
        } finally {
            env.close();
        }
    }
}
