package io.temporal.docs.workers.cloudrun;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface GreetingActivities {
  String composeGreeting(String name);
}
