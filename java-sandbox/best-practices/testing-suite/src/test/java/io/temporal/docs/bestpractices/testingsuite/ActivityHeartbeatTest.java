package io.temporal.docs.bestpractices.testingsuite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.temporal.testing.TestActivityEnvironment;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class ActivityHeartbeatTest {

  // @@@SNIPSTART java-testing-suite-listen-heartbeats
  @Test
  void testActivityHeartbeat() {
    TestActivityEnvironment env = TestActivityEnvironment.newInstance();

    AtomicInteger heartbeatCount = new AtomicInteger(0);

    env.setActivityHeartbeatListener(
        Void.class,
        heartbeat -> heartbeatCount.incrementAndGet());

    env.registerActivitiesImplementations(new GreetActivitiesImpl());

    GreetActivities activities = env.newActivityStub(GreetActivities.class);

    String result = activities.greet("Temporal");

    assertEquals("Hello Temporal!", result);
    assertEquals(5, heartbeatCount.get());
  }
  // @@@SNIPEND
}
