package io.temporal.docs.client.namespaces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.protobuf.util.Durations;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.temporal.api.enums.v1.NamespaceState;
import io.temporal.api.workflowservice.v1.DescribeNamespaceResponse;
import io.temporal.api.workflowservice.v1.RegisterNamespaceRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowServiceException;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The in-memory test server doesn't implement the Namespace APIs, so these tests use a Temporal
 * Service. Set TEMPORAL_ADDRESS to use an address other than localhost:7233. The tests are skipped
 * when no server is reachable.
 */
public class NamespaceExamplesTest {

    private static WorkflowServiceStubs service;

    @BeforeAll
    public static void setUp() {
        String address = System.getenv().getOrDefault("TEMPORAL_ADDRESS", "localhost:7233");
        service =
            WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder().setTarget(address).build());
        boolean reachable;
        try {
            service.healthCheck();
            reachable = true;
        } catch (RuntimeException e) {
            reachable = false;
        }
        assumeTrue(reachable, "No Temporal Service at " + address);
    }

    @AfterAll
    public static void tearDown() {
        if (service != null) {
            service.shutdownNow();
        }
    }

    private static String newName() {
        return "docs-ns-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    public void registerUpdateDescribeListDeprecateDelete() {
        String name = newName();
        NamespaceExamples.createNamespace(service, name);

        DescribeNamespaceResponse registered = NamespaceExamples.describeNamespace(service, name);
        assertEquals(Durations.fromDays(3), registered.getConfig().getWorkflowExecutionRetentionTtl());

        NamespaceExamples.updateNamespace(service, name);
        DescribeNamespaceResponse updated = NamespaceExamples.describeNamespace(service, name);
        assertEquals("your updated namespace description", updated.getNamespaceInfo().getDescription());
        assertEquals(Durations.fromHours(30), updated.getConfig().getWorkflowExecutionRetentionTtl());

        assertTrue(
            NamespaceExamples.listNamespaces(service).getNamespacesList().stream()
                .anyMatch(n -> n.getNamespaceInfo().getName().equals(name)));

        NamespaceExamples.deprecateNamespace(service, name);
        assertEquals(
            NamespaceState.NAMESPACE_STATE_DEPRECATED,
            NamespaceExamples.describeNamespace(service, name).getNamespaceInfo().getState());

        // New Workflow Executions can't start on a deprecated Namespace.
        WorkflowStub stub =
            WorkflowClient.newInstance(
                    service, WorkflowClientOptions.newBuilder().setNamespace(name).build())
                .newUntypedWorkflowStub(
                    "AnyWorkflow", WorkflowOptions.newBuilder().setTaskQueue("unused").build());
        assertThrows(WorkflowServiceException.class, stub::start);

        String deleted = NamespaceExamples.deleteNamespace(service, name).getDeletedNamespace();
        assertTrue(deleted.startsWith(name), deleted);
    }

    @Test
    public void registerWithoutRetentionPeriod() {
        String name = newName();
        RegisterNamespaceRequest req = RegisterNamespaceRequest.newBuilder().setNamespace(name).build();
        StatusRuntimeException e =
            assertThrows(StatusRuntimeException.class, () -> service.blockingStub().registerNamespace(req));
        assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
    }

    @Test
    public void localNamespaceMinimumRetentionIsOneHour() {
        RegisterNamespaceRequest oneHour =
            RegisterNamespaceRequest.newBuilder()
                .setNamespace(newName())
                .setWorkflowExecutionRetentionPeriod(Durations.fromHours(1))
                .build();
        service.blockingStub().registerNamespace(oneHour);

        RegisterNamespaceRequest thirtyMinutes =
            RegisterNamespaceRequest.newBuilder()
                .setNamespace(newName())
                .setWorkflowExecutionRetentionPeriod(Durations.fromMinutes(30))
                .build();
        StatusRuntimeException e =
            assertThrows(
                StatusRuntimeException.class,
                () -> service.blockingStub().registerNamespace(thirtyMinutes));
        assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
    }
}
