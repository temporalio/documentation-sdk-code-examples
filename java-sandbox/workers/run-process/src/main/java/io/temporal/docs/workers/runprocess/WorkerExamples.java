package io.temporal.docs.workers.runprocess;

import io.temporal.client.WorkflowClient;
import io.temporal.common.VersioningBehavior;
import io.temporal.common.WorkerDeploymentVersion;
import io.temporal.docs.workers.runprocess.RegisterTypesExample.GreetingActivitiesImpl;
import io.temporal.docs.workers.runprocess.RegisterTypesExample.GreetingWorkflow;
import io.temporal.docs.workers.runprocess.RegisterTypesExample.GreetingWorkflowImpl;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerDeploymentOptions;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import io.temporal.workflow.WorkflowVersioningBehavior;
import java.util.concurrent.TimeUnit;

public class WorkerExamples {

    /** GreetingWorkflow implementation pinned to the Worker Deployment Version it started on. */
    public static class VersionedGreetingWorkflowImpl implements GreetingWorkflow {
        @Override
        @WorkflowVersioningBehavior(VersioningBehavior.PINNED)
        public String greet(String name) {
            return "Hello " + name + "!";
        }
    }

    public static WorkerFactory createWorker(WorkflowClient client) {
        // @@@SNIPSTART java-create-worker
        WorkerFactory factory = WorkerFactory.newInstance(client);

        Worker worker = factory.newWorker("my-task-queue");
        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
        worker.registerActivitiesImplementations(new GreetingActivitiesImpl());

        factory.start();
        // @@@SNIPEND
        return factory;
    }

    public static Worker createVersionedWorker(WorkerFactory factory) {
        // @@@SNIPSTART java-versioned-worker
        WorkerOptions options =
            WorkerOptions.newBuilder()
                .setDeploymentOptions(
                    WorkerDeploymentOptions.newBuilder()
                        .setVersion(new WorkerDeploymentVersion("my-app", "1.0"))
                        .setUseVersioning(true)
                        .build())
                .build();

        Worker worker = factory.newWorker("my-task-queue", options);
        worker.registerWorkflowImplementationTypes(VersionedGreetingWorkflowImpl.class);
        worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
        // @@@SNIPEND
        return worker;
    }

    public static void shutDown(WorkerFactory factory) {
        // @@@SNIPSTART java-worker-graceful-shutdown
        factory.shutdown();
        factory.awaitTermination(30, TimeUnit.SECONDS);
        // @@@SNIPEND
    }

    public static Worker newWorkerThatHeartbeatsDuringShutdown(WorkerFactory factory) {
        // @@@SNIPSTART java-worker-heartbeat-during-shutdown
        WorkerOptions options =
            WorkerOptions.newBuilder().setAllowActivityHeartbeatDuringShutdown(true).build();
        Worker worker = factory.newWorker("your-task-queue", options);
        // @@@SNIPEND
        return worker;
    }
}
