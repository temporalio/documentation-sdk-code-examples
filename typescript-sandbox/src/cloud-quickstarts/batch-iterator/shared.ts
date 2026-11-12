/** Keep the Worker and client pointed at the same value. */
export const TASK_QUEUE =
  process.env.TEMPORAL_TASK_QUEUE || "quickstart-batch-iterator";

/** Records loaded and processed per Workflow run (one page). */
export const PAGE_SIZE = 5;
