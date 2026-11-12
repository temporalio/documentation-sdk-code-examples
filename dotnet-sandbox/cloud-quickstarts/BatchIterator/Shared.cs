namespace TemporalioDocs.CloudQuickstarts.BatchIterator;

/// <summary>Shared constants for the Worker and client.</summary>
public static class Shared
{
    /// <summary>Keep the Worker and client pointed at the same value.</summary>
    public static readonly string TaskQueue =
        Environment.GetEnvironmentVariable("TEMPORAL_TASK_QUEUE") is { Length: > 0 } taskQueue
            ? taskQueue
            : "quickstart-batch-iterator";

    /// <summary>Records loaded and processed per Workflow run (one page).</summary>
    public const int PageSize = 5;
}

/// <summary>One record in the dataset being processed.</summary>
public record SingleRecord(int Id);
