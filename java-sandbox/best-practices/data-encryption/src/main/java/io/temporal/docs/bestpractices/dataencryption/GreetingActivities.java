package io.temporal.docs.bestpractices.dataencryption;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface GreetingActivities {
    String composeGreeting(String greeting, String name);
}
