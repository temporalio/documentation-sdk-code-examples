package io.temporal.docs.activities.basics;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

public class ActivityTypeExample {

    public static class DefaultName {
        // @@@SNIPSTART java-activity-basics-type-default
        @ActivityInterface
        public interface GreetingActivities {
            String sendMessage(String input);

            @ActivityMethod
            String composeGreeting(String greeting, String language);
        }
        // @@@SNIPEND
    }

    public static class Prefix {
        // @@@SNIPSTART java-activity-basics-type-prefix
        @ActivityInterface(namePrefix = "Messaging_")
        public interface GreetingActivities {
            String sendMessage(String input);

            @ActivityMethod
            String composeGreeting(String greeting, String language);
        }
        // @@@SNIPEND
    }

    public static class CustomName {
        // @@@SNIPSTART java-activity-basics-type-custom-name
        @ActivityInterface(namePrefix = "Messaging_")
        public interface GreetingActivities {
            String sendMessage(String input);

            @ActivityMethod
            String composeGreeting(String greeting, String language);
            @ActivityMethod(name = "farewell")
            String composeFarewell(String farewell, String language);
        }
        // @@@SNIPEND
    }
}
