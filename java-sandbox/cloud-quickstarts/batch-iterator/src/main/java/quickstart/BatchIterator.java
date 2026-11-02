package quickstart;

/** Shared constants for the Worker and client. */
public final class BatchIterator {
    /** Keep the Worker and client pointed at the same value. */
    public static final String TASK_QUEUE =
            System.getenv().getOrDefault("TEMPORAL_TASK_QUEUE", "quickstart-batch-iterator");

    /** Records loaded and processed per Workflow run (one page). */
    public static final int PAGE_SIZE = 5;

    private BatchIterator() {}
}
