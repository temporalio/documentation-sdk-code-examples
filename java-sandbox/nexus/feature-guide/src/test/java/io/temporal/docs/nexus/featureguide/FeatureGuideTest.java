package io.temporal.docs.nexus.featureguide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.temporal.api.enums.v1.EventType;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.docs.nexus.featureguide.caller.EchoCallerWorkflow;
import io.temporal.docs.nexus.featureguide.caller.EchoCallerWorkflowImpl;
import io.temporal.docs.nexus.featureguide.caller.HelloCallerWorkflow;
import io.temporal.docs.nexus.featureguide.caller.HelloCallerWorkflowImpl;
import io.temporal.docs.nexus.featureguide.handler.HelloHandlerWorkflowImpl;
import io.temporal.docs.nexus.featureguide.handler.SampleNexusServiceImpl;
import io.temporal.docs.nexus.featureguide.messaging.GreetingNexusService;
import io.temporal.docs.nexus.featureguide.messaging.GreetingNexusServiceImpl;
import io.temporal.docs.nexus.featureguide.messaging.GreetingWorkflow;
import io.temporal.docs.nexus.featureguide.messaging.GreetingWorkflowImpl;
import io.temporal.docs.nexus.featureguide.service.SampleNexusService;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkflowImplementationOptions;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FeatureGuideTest {

  private static final String HANDLER_QUEUE = "my-handler-task-queue";
  private static final String CALLER_QUEUE = "my-caller-workflow-task-queue";

  private TestWorkflowEnvironment testEnv;
  private WorkflowClient client;

  @BeforeEach
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    client = testEnv.getWorkflowClient();
  }

  @AfterEach
  public void tearDown() {
    testEnv.close();
  }

  private static WorkflowImplementationOptions endpoint(String service, String endpoint) {
    return WorkflowImplementationOptions.newBuilder()
        .setNexusServiceOptions(
            Map.of(service, NexusServiceOptions.newBuilder().setEndpoint(endpoint).build()))
        .build();
  }

  private void startSampleWorkers(Object serviceImpl, Class<?> handlerWorkflow) {
    Worker handler = testEnv.newWorker(HANDLER_QUEUE);
    handler.registerWorkflowImplementationTypes(handlerWorkflow);
    handler.registerNexusServiceImplementation(serviceImpl);
    testEnv.createNexusEndpoint("my-nexus-endpoint-name", HANDLER_QUEUE);

    Worker caller = testEnv.newWorker(CALLER_QUEUE);
    caller.registerWorkflowImplementationTypes(
        endpoint("SampleNexusService", "my-nexus-endpoint-name"),
        EchoCallerWorkflowImpl.class,
        HelloCallerWorkflowImpl.class);
    testEnv.start();
  }

  private List<EventType> eventTypes(String workflowId) {
    return client.fetchHistory(workflowId).getHistory().getEventsList().stream()
        .map(HistoryEvent::getEventType)
        .toList();
  }

  @Test
  public void syncEchoAndAsyncHello() {
    startSampleWorkers(new SampleNexusServiceImpl(), HelloHandlerWorkflowImpl.class);

    EchoCallerWorkflow echo =
        client.newWorkflowStub(
            EchoCallerWorkflow.class,
            WorkflowOptions.newBuilder().setWorkflowId("echo").setTaskQueue(CALLER_QUEUE).build());
    assertEquals("Nexus Echo 👋", echo.echo("Nexus Echo 👋"));
    List<EventType> echoEvents = eventTypes("echo");
    assertTrue(echoEvents.contains(EventType.EVENT_TYPE_NEXUS_OPERATION_SCHEDULED));
    assertTrue(echoEvents.contains(EventType.EVENT_TYPE_NEXUS_OPERATION_COMPLETED));
    // A synchronous Operation has no Started event in the caller's history.
    assertFalse(echoEvents.contains(EventType.EVENT_TYPE_NEXUS_OPERATION_STARTED));

    HelloCallerWorkflow hello =
        client.newWorkflowStub(
            HelloCallerWorkflow.class,
            WorkflowOptions.newBuilder().setWorkflowId("hello").setTaskQueue(CALLER_QUEUE).build());
    assertEquals("¡Hola! Nexus 👋", hello.hello("Nexus", SampleNexusService.Language.ES));
    assertTrue(eventTypes("hello").contains(EventType.EVENT_TYPE_NEXUS_OPERATION_STARTED));
    // The handler Workflow ID comes from the Operation input.
    assertEquals(
        "¡Hola! Nexus 👋",
        client.newUntypedWorkflowStub("hello-Nexus-es").getResult(SampleNexusService.HelloOutput.class)
            .getMessage());
  }

  @Test
  public void multipleWorkflowArguments() {
    startSampleWorkers(
        new io.temporal.docs.nexus.featureguide.multipleargs.SampleNexusServiceImpl(),
        io.temporal.docs.nexus.featureguide.multipleargs.HelloHandlerWorkflowImpl.class);

    HelloCallerWorkflow hello =
        client.newWorkflowStub(
            HelloCallerWorkflow.class, WorkflowOptions.newBuilder().setTaskQueue(CALLER_QUEUE).build());
    assertEquals("FR hello Nexus", hello.hello("Nexus", SampleNexusService.Language.FR));
  }

  @WorkflowInterface
  public interface ApproveCallerWorkflow {
    @WorkflowMethod
    void approve(String userId, String name);
  }

  public static class ApproveCallerWorkflowImpl implements ApproveCallerWorkflow {
    private final GreetingNexusService service =
        Workflow.newNexusServiceStub(
            GreetingNexusService.class,
            NexusServiceOptions.newBuilder()
                .setOperationOptions(
                    NexusOperationOptions.newBuilder()
                        .setScheduleToCloseTimeout(Duration.ofSeconds(10))
                        .build())
                .build());

    @Override
    public void approve(String userId, String name) {
      service.approve(new GreetingNexusService.ApproveInput(userId, name));
    }
  }

  @Test
  public void signalRunningWorkflowThroughNexus() {
    Worker handler = testEnv.newWorker(HANDLER_QUEUE);
    handler.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    handler.registerNexusServiceImplementation(new GreetingNexusServiceImpl());
    testEnv.createNexusEndpoint("greeting-endpoint", HANDLER_QUEUE);

    Worker caller = testEnv.newWorker(CALLER_QUEUE);
    caller.registerWorkflowImplementationTypes(
        endpoint("GreetingNexusService", "greeting-endpoint"), ApproveCallerWorkflowImpl.class);
    testEnv.start();

    GreetingWorkflow greeting =
        client.newWorkflowStub(
            GreetingWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("GreetingWorkflow_for_user-1")
                .setTaskQueue(HANDLER_QUEUE)
                .build());
    WorkflowClient.start(greeting::run);

    client
        .newWorkflowStub(
            ApproveCallerWorkflow.class, WorkflowOptions.newBuilder().setTaskQueue(CALLER_QUEUE).build())
        .approve("user-1", "Ann");
    assertEquals("Approved by Ann", WorkflowStub.fromTyped(greeting).getResult(String.class));
  }
}
