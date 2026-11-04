package io.temporal.docs.platform.observability.searchattributes;

import io.temporal.failure.ApplicationFailure;

public class PizzaActivitiesImpl implements PizzaActivities {
    @Override
    public int getDistance(String address) {
        if (address == null || address.isBlank()) {
            throw ApplicationFailure.newNonRetryableFailure("Unknown address", "InvalidAddress");
        }
        return 5;
    }
}
