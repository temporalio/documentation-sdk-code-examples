package io.temporal.docs.workers.interceptors;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerFactoryOptions;

public class RegisterInterceptors {

    public static WorkflowClient newClient(WorkflowServiceStubs service) {
        // @@@SNIPSTART java-interceptors-register-client
        WorkflowClientOptions clientOptions =
            WorkflowClientOptions.newBuilder().setInterceptors(new TracingClientInterceptor()).build();
        WorkflowClient client = WorkflowClient.newInstance(service, clientOptions);
        // @@@SNIPEND
        return client;
    }

    public static WorkerFactory newWorkerFactory(WorkflowClient client) {
        // @@@SNIPSTART java-interceptors-register-worker
        WorkerFactoryOptions factoryOptions =
            WorkerFactoryOptions.newBuilder().setWorkerInterceptors(new TracingWorkerInterceptor()).build();
        WorkerFactory factory = WorkerFactory.newInstance(client, factoryOptions);
        // @@@SNIPEND
        return factory;
    }
}
