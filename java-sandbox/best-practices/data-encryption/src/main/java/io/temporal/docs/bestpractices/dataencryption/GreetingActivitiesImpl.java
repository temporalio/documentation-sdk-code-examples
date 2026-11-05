package io.temporal.docs.bestpractices.dataencryption;

public class GreetingActivitiesImpl implements GreetingActivities {
    @Override
    public String composeGreeting(String greeting, String name) {
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name is empty for greeting " + greeting);
        }
        return greeting + " " + name + "!";
    }
}
