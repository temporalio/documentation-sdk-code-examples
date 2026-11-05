package io.temporal.docs.bestpractices.testingsuite;

import static org.junit.Assert.assertEquals;

import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

// @@@SNIPSTART java-testing-suite-junit4-rule
public class SayHelloWorkflowJUnit4Test {
  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(SayHelloWorkflowImpl.class)
          .setActivityImplementations(new GreetActivitiesImpl())
          .build();

  @Test
  public void testSayHello() {
    // Get a workflow stub using the same task queue the worker uses.
    SayHelloWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                SayHelloWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.sayHello("Temporal");
    assertEquals("Hello Temporal!", greeting);
  }
}
// @@@SNIPEND
