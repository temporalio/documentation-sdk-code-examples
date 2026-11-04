package io.temporal.docs.platform.observability.searchattributes;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface PizzaActivities {
    // Returns the delivery distance to the address, in kilometers.
    int getDistance(String address);
}
