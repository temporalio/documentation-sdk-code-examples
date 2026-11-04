package io.temporal.docs.platform.observability.tracing;

import io.opentracing.Tracer;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.opentracing.OpenTracingClientInterceptor;
import io.temporal.opentracing.OpenTracingOptions;
import io.temporal.opentracing.OpenTracingWorkerInterceptor;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerFactoryOptions;

public class TracingExample {

    // Builds OpenTracingOptions for any io.opentracing.Tracer, such as a Jaeger tracer or the
    // OpenTelemetry OpenTracing shim.
    public static OpenTracingOptions tracingOptions(Tracer tracer) {
        // @@@SNIPSTART java-observability-tracing-options
        OpenTracingOptions tracingOptions = OpenTracingOptions.newBuilder().setTracer(tracer).build();
        // @@@SNIPEND
        return tracingOptions;
    }

    public static WorkflowClient createClient(
            WorkflowServiceStubs service, OpenTracingOptions tracingOptions) {
        // @@@SNIPSTART java-observability-tracing-client
        WorkflowClientOptions clientOptions =
            WorkflowClientOptions.newBuilder()
                .setInterceptors(new OpenTracingClientInterceptor(tracingOptions))
                .build();
        WorkflowClient client = WorkflowClient.newInstance(service, clientOptions);
        // @@@SNIPEND
        return client;
    }

    public static WorkerFactory createWorkerFactory(
            WorkflowClient client, OpenTracingOptions tracingOptions) {
        // @@@SNIPSTART java-observability-tracing-worker
        WorkerFactoryOptions factoryOptions =
            WorkerFactoryOptions.newBuilder()
                .setWorkerInterceptors(new OpenTracingWorkerInterceptor(tracingOptions))
                .build();
        WorkerFactory factory = WorkerFactory.newInstance(client, factoryOptions);
        // @@@SNIPEND
        return factory;
    }
}
