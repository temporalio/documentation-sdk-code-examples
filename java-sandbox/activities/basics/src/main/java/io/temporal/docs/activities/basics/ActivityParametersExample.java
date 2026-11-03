package io.temporal.docs.activities.basics;

import io.temporal.activity.ActivityInterface;

public class ActivityParametersExample {

    public static class CustomObj {
        public String name;

        public CustomObj() {}

        public CustomObj(String name) {
            this.name = name;
        }
    }

    // @@@SNIPSTART java-activity-basics-parameters
    @ActivityInterface
    public interface YourActivities {
        String getCustomObject(CustomObj customobj);
        void sendCustomObject(CustomObj customobj, String abc);
    }
    // @@@SNIPEND

    public static class YourActivitiesImpl implements YourActivities {
        @Override
        public String getCustomObject(CustomObj customobj) {
            return customobj.name;
        }

        @Override
        public void sendCustomObject(CustomObj customobj, String abc) {}
    }
}
