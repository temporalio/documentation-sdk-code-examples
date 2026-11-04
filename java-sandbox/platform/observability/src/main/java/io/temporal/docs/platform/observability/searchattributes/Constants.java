package io.temporal.docs.platform.observability.searchattributes;

import io.temporal.common.SearchAttributeKey;

// @@@SNIPSTART java-observability-search-attribute-key
// In a shared constants file, so all files have access
public class Constants {
    public static final String TASK_QUEUE_NAME = "pizza-tasks";

    public static final SearchAttributeKey<Boolean> IS_ORDER_FAILED =
        SearchAttributeKey.forBoolean("isOrderFailed");
}
// @@@SNIPEND
