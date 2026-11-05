package io.temporal.docs.bestpractices.errorhandling;

import static io.temporal.docs.bestpractices.errorhandling.GreetingService.callExternalService;

import io.temporal.activity.Activity;
import java.io.IOException;

// @@@SNIPSTART java-error-handling-wrap-checked-exception
public class GreetingActivitiesImpl implements GreetingActivities {
    @Override
    public String composeGreeting(String greeting, String name) {
        try {
            return callExternalService(greeting, name); // declares `throws IOException`
        } catch (IOException e) {
            throw Activity.wrap(e);
        }
    }
}
// @@@SNIPEND
