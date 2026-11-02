package quickstart;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates records in memory so the sample needs no external storage. A real application would
 * page through a database table, a file, or an object store here.
 */
public final class RecordLoaderImpl implements RecordLoader {

    /** The dataset is always 3 pages long. */
    public static final int PAGE_COUNT = 3;

    @Override
    public List<SingleRecord> getRecords(int pageSize, int offset) {
        List<SingleRecord> records = new ArrayList<>(pageSize);
        if (offset < pageSize * PAGE_COUNT) {
            for (int i = 0; i < pageSize; i++) {
                records.add(new SingleRecord(offset + i));
            }
        }
        return records;
    }
}
