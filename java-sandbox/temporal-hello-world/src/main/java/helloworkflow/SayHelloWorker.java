package helloworkflow;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class SayHelloWorker {

    public static void main(String[] args) {

      WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
      WorkflowClient client = WorkflowClient.newInstance(service);
      WorkerFactory factory = WorkerFactory.newInstance(client);

      Worker worker = factory.newWorker("my-task-queue");
      worker.registerWorkflowImplementationTypes(SayHelloWorkflowImpl.class);
      worker.registerActivitiesImplementations(new GreetActivitiesImpl());

      System.out.println("Starting SayHelloWorker for task queue 'my-task-queue'...");

      factory.start();

    }

}