package io.temporal.docs.activities.basics;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

public class DefineActivityExample {

    public static class Unannotated {
        // @@@SNIPSTART java-activity-basics-interface
        @ActivityInterface
        public interface GreetingActivities {
            String composeGreeting(String greeting, String language);
        }
        // @@@SNIPEND
    }

    // @@@SNIPSTART java-activity-basics-activity-method
    @ActivityInterface
    public interface GreetingActivities {
        @ActivityMethod
        String composeGreeting(String greeting, String language);
    }
    // @@@SNIPEND

    // @@@SNIPSTART java-activity-basics-implementation
    // Implementation for the GreetingActivities interface example from in the previous section
    static class GreetingActivitiesImpl implements GreetingActivities {
        @Override
        public String composeGreeting(String greeting, String name) {
            return greeting + " " + name + "!";
        }
    }
    // @@@SNIPEND

    public static GreetingActivities newImplementation() {
        return new GreetingActivitiesImpl();
    }
}
