// @@@SNIPSTART java-testing-suite-run-activity
package io.temporal.docs.bestpractices.testingsuite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.temporal.testing.TestActivityEnvironment;
import org.junit.jupiter.api.Test;

public class GreetActivitiesTest {

  @Test
  public void testActivityImpl() {
    TestActivityEnvironment testEnv = TestActivityEnvironment.newInstance();

    testEnv.registerActivitiesImplementations(new GreetActivitiesImpl());

    GreetActivities activities = testEnv.newActivityStub(GreetActivities.class);

    String result = activities.greet("Temporal");

    assertEquals("Hello Temporal!", result);
  }
}
// @@@SNIPEND
