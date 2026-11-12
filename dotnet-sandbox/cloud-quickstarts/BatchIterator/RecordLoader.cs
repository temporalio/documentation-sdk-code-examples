using Temporalio.Activities;

namespace TemporalioDocs.CloudQuickstarts.BatchIterator;

/// <summary>
/// Generates records in memory so the sample needs no external storage. A real
/// application would page through a database table, a file, or an object store here.
/// </summary>
public static class RecordLoader
{
    /// <summary>The dataset is always 3 pages long.</summary>
    public const int PageCount = 3;

    /// <summary>Returns the next page of records, or an empty list when there are none left.</summary>
    [Activity]
    public static List<SingleRecord> GetRecords(int pageSize, int offset) =>
        offset < pageSize * PageCount
            ? Enumerable.Range(offset, pageSize).Select(id => new SingleRecord(id)).ToList()
            : [];
}
