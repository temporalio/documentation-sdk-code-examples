package io.temporal.docs.platform.enrichingui;

public class YourActivitiesImpl implements YourActivities {
    @Override
    public String yourActivity(String input) {
        return "Processed " + input;
    }
}
