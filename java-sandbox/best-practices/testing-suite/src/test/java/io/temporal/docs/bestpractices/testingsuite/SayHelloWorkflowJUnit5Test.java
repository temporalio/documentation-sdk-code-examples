package io.temporal.docs.bestpractices.testingsuite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

// @@@SNIPSTART java-testing-suite-junit5-extension
public class SayHelloWorkflowJUnit5Test {
  @RegisterExtension
  public static final TestWorkflowExtension testWorkflowExtension =
      TestWorkflowExtension.newBuilder()
          .setWorkflowTypes(SayHelloWorkflowImpl.class)
          .setActivityImplementations(new GreetActivitiesImpl())
          .build();

  @Test
  public void testSayHello(
      TestWorkflowEnvironment testEnv, Worker worker, SayHelloWorkflow workflow) {
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.sayHello("Temporal");
    assertEquals("Hello Temporal!", greeting);
  }
}
// @@@SNIPEND
