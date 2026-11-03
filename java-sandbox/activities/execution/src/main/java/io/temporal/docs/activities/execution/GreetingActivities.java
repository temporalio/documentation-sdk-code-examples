package io.temporal.docs.activities.execution;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface GreetingActivities {
    String composeGreeting(String greeting, String name);

    String getCustomerGreeting(String name);

    String emailCustomerGreeting(String name);
}
