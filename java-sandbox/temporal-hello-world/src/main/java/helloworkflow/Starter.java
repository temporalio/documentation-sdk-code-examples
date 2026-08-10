package helloworkflow;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class Starter {
    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        SayHelloWorkflow workflow = client.newWorkflowStub(
            SayHelloWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue("my-task-queue")
                .setWorkflowId("say-hello-workflow-id")
                .build()
        );

        String result = workflow.sayHello("Temporal");
        System.out.println("Workflow result: " + result);

        service.shutdown();
        service.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);
    }
}