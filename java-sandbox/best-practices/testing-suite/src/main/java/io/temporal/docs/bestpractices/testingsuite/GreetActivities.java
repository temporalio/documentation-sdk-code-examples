package io.temporal.docs.bestpractices.testingsuite;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface GreetActivities {
  String greet(String name);
}
