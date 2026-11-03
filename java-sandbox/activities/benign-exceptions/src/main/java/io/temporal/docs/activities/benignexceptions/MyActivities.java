package io.temporal.docs.activities.benignexceptions;

// @@@SNIPSTART java-benign-exceptions-interface
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface MyActivities {
    @ActivityMethod
    String myActivity();
}
// @@@SNIPEND
