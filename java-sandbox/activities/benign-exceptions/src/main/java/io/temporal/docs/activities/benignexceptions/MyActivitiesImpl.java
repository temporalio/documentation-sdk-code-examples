package io.temporal.docs.activities.benignexceptions;

// @@@SNIPSTART java-benign-exceptions-impl
import io.temporal.failure.ApplicationErrorCategory;
import io.temporal.failure.ApplicationFailure;

public class MyActivitiesImpl implements MyActivities {
    @Override
    public String myActivity() {
        try {
            return callExternalService();
        } catch (Exception e) {
            // Mark this error as benign since it's expected
            throw ApplicationFailure.newBuilder()
                .setMessage(e.getMessage())
                .setType(e.getClass().getName())
                .setCause(e)
                .setCategory(ApplicationErrorCategory.BENIGN)
                .build();
        }
    }

    // Stands in for a call to an external service that isn't ready yet.
    protected String callExternalService() throws Exception {
        throw new IllegalStateException("Service isn't ready yet");
    }
}
// @@@SNIPEND
