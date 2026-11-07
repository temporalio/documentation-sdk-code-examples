package io.temporal.docs.bestpractices.errorhandling;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface GreetingActivities {
    String composeGreeting(String greeting, String name);
}
