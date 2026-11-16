package quickstart;

import io.temporal.client.WorkflowClient;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.time.Instant;
import quickstart.Models.ApprovalDecision;

public class SendApproval {
    private static final String USAGE =
            "Usage: mvn -q compile exec:java@send-approval -Dexec.args=\"<approve|reject> [notes]\"";

    public static void main(String[] args) throws Exception {
        if (args.length < 1
                || !(args[0].equalsIgnoreCase("approve") || args[0].equalsIgnoreCase("reject"))) {
            System.out.println(USAGE);
            System.exit(1);
        }
        boolean approved = args[0].equalsIgnoreCase("approve");
        String notes = args.length > 1 ? args[1] : null;

        // Same environment-configuration lookup the Worker and client use.
        ClientConfigProfile profile = ClientConfigProfile.load();
        WorkflowServiceStubs service =
                WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());
        WorkflowClient client = WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());

        // Deliver the reviewer's decision to the waiting Workflow as a Signal. With no
        // request_id, it applies to whichever request is pending.
        HumanInTheLoopWorkflow workflow =
                client.newWorkflowStub(HumanInTheLoopWorkflow.class, HumanInTheLoop.WORKFLOW_ID);
        workflow.approvalDecision(
                new ApprovalDecision(approved, null, notes, Instant.now().toString()));

        System.out.printf(
                "Sent %s decision to Workflow %s%n",
                approved ? "approve" : "reject", HumanInTheLoop.WORKFLOW_ID);
        service.shutdown();
    }
}
