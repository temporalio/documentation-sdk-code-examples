package io.temporal.docs.activities.standaloneactivities;

// @@@SNIPSTART java-standalone-activities-interface
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/** Activity interface shared by all programs in this sample. */
@ActivityInterface
public interface GreetingActivities {

  @ActivityMethod
  String composeGreeting(String greeting, String name);
}
// @@@SNIPEND
