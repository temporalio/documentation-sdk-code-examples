package quickstart;

import io.temporal.activity.ActivityInterface;
import java.util.List;

@ActivityInterface
public interface RecordLoader {
    /**
     * Returns the next page of records.
     *
     * @param pageSize maximum number of records to return.
     * @param offset offset of the next page.
     * @return an empty list when there are no more records to process.
     */
    List<SingleRecord> getRecords(int pageSize, int offset);
}
