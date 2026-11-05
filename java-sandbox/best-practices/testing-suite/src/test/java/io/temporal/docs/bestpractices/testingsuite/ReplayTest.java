package io.temporal.docs.bestpractices.testingsuite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReplayTest {
  private static final String TASK_QUEUE = "mytaskqueue";

  private TestWorkflowEnvironment testEnv;
  private WorkflowClient client;

  // Returns right away so the Workflow under replay finishes quickly.
  public static class FastGreetActivities implements GreetActivities {
    @Override
    public String greet(String name) {
      return "Hello " + name + "!";
    }
  }

  @BeforeEach
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    Worker worker = testEnv.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(SayHelloWorkflowImpl.class);
    worker.registerActivitiesImplementations(new FastGreetActivities());
    testEnv.start();
    client = testEnv.getWorkflowClient();
  }

  @AfterEach
  public void tearDown() {
    testEnv.close();
  }

  private void runWorkflow(String workflowId) {
    SayHelloWorkflow workflow = client.newWorkflowStub(SayHelloWorkflow.class,
        WorkflowOptions.newBuilder().setWorkflowId(workflowId).setTaskQueue(TASK_QUEUE).build());
    assertEquals("Hello Temporal!", workflow.sayHello("Temporal"));
  }

  @Test
  public void replayHistoryFromAFile() throws Exception {
    runWorkflow("replay-file");
    Path file = Path.of("my_history.json");
    Files.writeString(file, client.fetchHistory("replay-file").toJson(true));
    try {
      ReplayExamples.replayFromFile();
    } finally {
      Files.deleteIfExists(file);
    }
  }
}
