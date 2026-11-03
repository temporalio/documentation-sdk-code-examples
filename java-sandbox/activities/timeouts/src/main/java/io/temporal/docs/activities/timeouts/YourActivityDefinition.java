package io.temporal.docs.activities.timeouts;

import io.temporal.activity.ActivityInterface;
import java.util.List;

@ActivityInterface
public interface YourActivityDefinition {
    String yourActivityMethod(YourActivityMethodParam param);

    class YourActivityMethodParam {
        public List<String> items;

        public YourActivityMethodParam() {}

        public YourActivityMethodParam(List<String> items) {
            this.items = items;
        }
    }
}
