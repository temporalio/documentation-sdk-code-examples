package io.temporal.docs.workers.cloudrun;

public final class GreetingActivitiesImpl implements GreetingActivities {
  @Override
  public String composeGreeting(String name) {
    return "Hello " + name + "!";
  }
}
