package io.temporal.docs.bestpractices.errorhandling;

import io.temporal.worker.WorkerFactoryOptions;

public class WorkerSetup {
    public static WorkerFactoryOptions factoryOptions() {
        // @@@SNIPSTART java-error-handling-register-interceptor
        WorkerFactoryOptions factoryOptions = WorkerFactoryOptions.newBuilder()
            .setWorkerInterceptors(new ErrorNormalizingWorkerInterceptor())
            .build();
        // @@@SNIPEND
        return factoryOptions;
    }
}
