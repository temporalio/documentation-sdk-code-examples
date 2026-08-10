package helloworkflow;

import java.util.concurrent.TimeUnit;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;
import io.temporal.client.ActivityCompletionException;

public class GreetActivitiesImpl implements GreetActivities {

    @Override
    public String greet(String name) {
      ActivityExecutionContext context = Activity.getExecutionContext();

      for (int i = 0; i < 5; i++) {
        try {
          context.heartbeat(null);
        } catch (ActivityCompletionException e) {
          throw e;
        }
        
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