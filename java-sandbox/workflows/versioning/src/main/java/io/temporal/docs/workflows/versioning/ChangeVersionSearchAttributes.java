package io.temporal.docs.workflows.versioning;

// @@@SNIPSTART java-versioning-search-attribute-key
import io.temporal.common.SearchAttributeKey;
import java.util.List;

public class ChangeVersionSearchAttributes {
    public static final SearchAttributeKey<List<String>> TEMPORAL_CHANGE_VERSION =
        SearchAttributeKey.forKeywordList("TemporalChangeVersion");
}
// @@@SNIPEND
