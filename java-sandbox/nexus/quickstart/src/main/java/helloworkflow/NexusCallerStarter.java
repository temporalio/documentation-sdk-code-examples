// @@@SNIPSTART java-nexus-quickstart-caller-starter
package helloworkflow;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkflowImplementationOptions;
import io.temporal.workflow.NexusServiceOptions;

import java.util.Collections;

public class NexusCallerStarter {

    public static void main(String[] args) {

        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service,
            WorkflowClientOptions.newBuilder()
                .setNamespace("my-caller-namespace")
                .build()
        );
        WorkerFactory factory = WorkerFactory.newInstance(client);

        Worker worker = factory.newWorker("my-caller-task-queue");
        worker.registerWorkflowImplementationTypes(
            WorkflowImplementationOptions.newBuilder()
                .setNexusServiceOptions(
                    Collections.singletonMap(
                        "SayHelloNexusService",
                        NexusServiceOptions.newBuilder()
                            .setEndpoint("my-nexus-endpoint-name")
                            .build()))
                .build(),
            NexusCallerWorkflowImpl.class
        );
        factory.start();

        NexusCallerWorkflow workflow = client.newWorkflowStub(
            NexusCallerWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue("my-caller-task-queue")
                .setWorkflowId("nexus-caller-workflow-id")
                .build()
        );

        String result = workflow.greetThroughNexus("Temporal");
        System.out.println("Workflow result: " + result);

        factory.shutdown();
        System.exit(0);

    }

}
// @@@SNIPEND
