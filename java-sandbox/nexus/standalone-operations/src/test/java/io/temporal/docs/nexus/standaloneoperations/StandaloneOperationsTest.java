package io.temporal.docs.nexus.standaloneoperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.temporal.api.nexus.v1.EndpointSpec;
import io.temporal.api.nexus.v1.EndpointTarget;
import io.temporal.api.operatorservice.v1.CreateNexusEndpointRequest;
import io.temporal.client.NexusClient;
import io.temporal.client.NexusClientOptions;
import io.temporal.client.NexusOperationExecutionCount;
import io.temporal.client.NexusServiceClient;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.docs.nexus.standaloneoperations.handler.GreetingNexusServiceImpl;
import io.temporal.docs.nexus.standaloneoperations.handler.GreetingWorkflowImpl;
import io.temporal.docs.nexus.standaloneoperations.service.GreetingNexusService;
import io.temporal.serviceclient.OperatorServiceStubs;
import io.temporal.serviceclient.OperatorServiceStubsOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The in-memory test server doesn't support Standalone Nexus Operations, so these tests need a dev
 * server with the feature enabled:
 *
 * <pre>
 * temporal server start-dev --dynamic-config-value nexusoperation.enableStandalone=true
 * </pre>
 *
 * Set TEMPORAL_ADDRESS to use an address other than localhost:7233. The tests create their own
 * Nexus Endpoint and Task Queue in the default Namespace, and are skipped when no server is
 * reachable or the feature is disabled.
 */
public class StandaloneOperationsTest {

  private static final String NAMESPACE = "default";

  private static WorkflowServiceStubs service;
  private static OperatorServiceStubs operator;
  private static WorkerFactory factory;
  private static String taskQueue;
  private static String endpointName;
  private static NexusClient nexusClient;
  private static NexusServiceClient<GreetingNexusService> greetingClient;

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

    String suffix = UUID.randomUUID().toString().substring(0, 8);
    operator =
        OperatorServiceStubs.newServiceStubs(
            OperatorServiceStubsOptions.newBuilder().setTarget(address).validateAndBuildWithDefaults());
    taskQueue = "standalone-ops-test-" + suffix;
    endpointName = createEndpoint("standalone-ops-test-" + suffix);

    WorkflowClient client =
        WorkflowClient.newInstance(
            service, WorkflowClientOptions.newBuilder().setNamespace(NAMESPACE).build());
    factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(taskQueue);
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    worker.registerNexusServiceImplementation(new GreetingNexusServiceImpl());
    factory.start();

    nexusClient =
        NexusClient.newInstance(
            service, NexusClientOptions.newBuilder().setNamespace(NAMESPACE).build());
    greetingClient = nexusClient.newNexusServiceClient(GreetingNexusService.class, endpointName);

    try {
      nexusClient.countNexusOperationExecutions(null);
    } catch (StatusRuntimeException e) {
      assumeTrue(
          e.getStatus().getCode() != Status.Code.UNIMPLEMENTED,
          "Standalone Nexus Operations are disabled: " + e.getMessage());
      throw e;
    }
  }

  private static String createEndpoint(String name) {
    operator
        .blockingStub()
        .createNexusEndpoint(
            CreateNexusEndpointRequest.newBuilder()
                .setSpec(
                    EndpointSpec.newBuilder()
                        .setName(name)
                        .setTarget(
                            EndpointTarget.newBuilder()
                                .setWorker(
                                    EndpointTarget.Worker.newBuilder()
                                        .setNamespace(NAMESPACE)
                                        .setTaskQueue(taskQueue))))
                .build());
    return name;
  }

  @AfterAll
  public static void tearDown() {
    if (factory != null) {
      factory.shutdownNow();
    }
    if (operator != null) {
      operator.shutdownNow();
    }
    if (service != null) {
      service.shutdownNow();
    }
  }

  @Test
  public void executeAndGetHandleById() throws Exception {
    GreetingNexusService.GreetingOutput output =
        StandaloneClientStarter.demonstrateExecuteAndGettingHandleById(nexusClient, greetingClient);
    assertEquals("Hello, execute!", output.getMessage());
  }

  @Test
  public void startAndGetResult() {
    assertEquals(
        "Hello, start!",
        StandaloneClientStarter.demonstrateStartAndGetResult(greetingClient).getMessage());
  }

  @Test
  public void cancelEndsWithNexusOperationException() {
    assertNotNull(StandaloneClientStarter.demonstrateStartAndCancel(greetingClient));
  }

  @Test
  public void listAndCountByEndpoint() throws Exception {
    // A separate Endpoint keeps the other tests' operations out of this one's counts.
    String visibilityEndpoint = createEndpoint(endpointName + "-visibility");
    NexusServiceClient<GreetingNexusService> client =
        nexusClient.newNexusServiceClient(GreetingNexusService.class, visibilityEndpoint);
    StandaloneClientStarter.demonstrateExecuteAndGettingHandleById(nexusClient, client);
    StandaloneClientStarter.demonstrateStartAndGetResult(client);
    StandaloneClientStarter.demonstrateStartAndCancel(client);

    // Visibility is eventually consistent, so wait for all four operations to show up.
    long deadline = System.currentTimeMillis() + 15_000;
    NexusOperationExecutionCount grouped;
    do {
      grouped = StandaloneClientStarter.demonstrateVisibility(nexusClient, visibilityEndpoint);
      if (grouped.getCount() == 4) {
        break;
      }
      Thread.sleep(300);
    } while (System.currentTimeMillis() < deadline);

    Map<String, Long> byStatus =
        grouped.getGroups().stream()
            .collect(
                Collectors.toMap(
                    g -> g.getGroupValues().get(0).toString(),
                    NexusOperationExecutionCount.AggregationGroup::getCount));
    assertEquals(4, grouped.getCount());
    assertEquals(Map.of("[Completed]", 3L, "[Canceled]", 1L), byStatus);
  }
}
