package quickstart;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import quickstart.Models.WorkflowInput;

public class Client {
    private static final String DEFAULT_REQUEST =
            "Delete all test data from the production database";

    public static void main(String[] args) throws Exception {
        String userRequest = args.length > 0 ? args[0] : DEFAULT_REQUEST;

        // Same environment-configuration lookup the Worker uses: TEMPORAL_PROFILE
        // selects the profile, the TOML file supplies address/namespace/credentials,
        // and TEMPORAL_* env vars can override individual fields.
        ClientConfigProfile profile = ClientConfigProfile.load();
        WorkflowServiceStubsOptions serviceOptions = profile.toWorkflowServiceStubsOptions();
        WorkflowClientOptions clientOptions = profile.toWorkflowClientOptions();

        WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(serviceOptions);
        WorkflowClient client = WorkflowClient.newInstance(service, clientOptions);

        HumanInTheLoopWorkflow workflow =
                client.newWorkflowStub(
                        HumanInTheLoopWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setWorkflowId(HumanInTheLoop.WORKFLOW_ID)
                                .setTaskQueue(HumanInTheLoop.TASK_QUEUE)
                                .build());

        try {
            WorkflowClient.start(workflow::run, new WorkflowInput(userRequest, 300));
        } catch (WorkflowExecutionAlreadyStarted e) {
            System.out.println(
                    "Workflow "
                            + HumanInTheLoop.WORKFLOW_ID
                            + " is already running. Approve or reject it, wait for it to time out,"
                            + " or terminate it:\n  temporal workflow terminate --workflow-id "
                            + HumanInTheLoop.WORKFLOW_ID);
            service.shutdown();
            System.exit(1);
        }

        System.out.printf(
                "Started Workflow %s against %s (namespace: %s)...%n",
                HumanInTheLoop.WORKFLOW_ID,
                serviceOptions.getTarget(),
                HumanInTheLoop.namespaceOf(clientOptions));
        System.out.println("Request: " + userRequest);
        System.out.println("If the action is risky, the Worker prints how to approve or reject it.");

        String result = WorkflowStub.fromTyped(workflow).getResult(String.class);
        System.out.println("Result: " + result);

        service.shutdown();
    }
}
