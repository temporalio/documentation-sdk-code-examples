package io.temporal.docs.workflows.cancellation;

import io.temporal.client.WorkflowStub;
import io.temporal.docs.workflows.cancellation.HelloCancellationScope.GreetingWorkflow;

public class InterruptWorkflowExample {

    public static void cancel(GreetingWorkflow workflow) {
        // @@@SNIPSTART java-cancellation-cancel-workflow
        WorkflowStub workflowStub = WorkflowStub.fromTyped(workflow);
        workflowStub.cancel();
        // @@@SNIPEND
    }

    public static void terminate(GreetingWorkflow myWorkflowStub) {
        // @@@SNIPSTART java-cancellation-terminate-workflow
        WorkflowStub untyped = WorkflowStub.fromTyped(myWorkflowStub);
        untyped.terminate("Sample reason");
        // @@@SNIPEND
    }
}
