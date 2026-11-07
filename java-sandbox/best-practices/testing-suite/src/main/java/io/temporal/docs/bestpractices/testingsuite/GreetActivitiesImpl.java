// @@@SNIPSTART java-testing-suite-greet-activities-impl
package io.temporal.docs.bestpractices.testingsuite;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;
import java.util.concurrent.TimeUnit;

public class GreetActivitiesImpl implements GreetActivities {

  @Override
  public String greet(String name) {
    ActivityExecutionContext context = Activity.getExecutionContext();

    for (int i = 0; i < 5; i++) {
      // Throws ActivityCompletionException when the Activity is canceled.
      context.heartbeat(null);
      sleep(1);
    }

    return "Hello " + name + "!";
  }

  private void sleep(int seconds) {
    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }
}
// @@@SNIPEND
