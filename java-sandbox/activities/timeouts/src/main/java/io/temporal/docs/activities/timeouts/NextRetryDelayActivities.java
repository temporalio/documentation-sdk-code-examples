package io.temporal.docs.activities.timeouts;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityInterface;
import io.temporal.failure.ApplicationFailure;
import java.time.Duration;

public class NextRetryDelayActivities {

    @ActivityInterface
    public interface FlakyActivities {
        String callService(int succeedOnAttempt);
    }

    public static class FlakyActivitiesImpl implements FlakyActivities {
        @Override
        public String callService(int succeedOnAttempt) {
            int attempt = Activity.getExecutionContext().getInfo().getAttempt();
            if (attempt < succeedOnAttempt) {
                failWithDelay();
            }
            return "succeeded on attempt " + attempt;
        }

        private void failWithDelay() {
            // @@@SNIPSTART java-activity-timeouts-next-retry-delay
            int attempt = Activity.getExecutionContext().getInfo().getAttempt();

            throw ApplicationFailure.newFailureWithCauseAndDelay(
                "Something bad happened on attempt " + attempt,
                "my_failure_type",
                null,
                Duration.ofSeconds(3L * attempt));
            // @@@SNIPEND
        }
    }
}
