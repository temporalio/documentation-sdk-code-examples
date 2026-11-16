package quickstart;

import io.temporal.client.WorkflowClientOptions;

/** Shared constants for the Worker and clients. */
public final class HumanInTheLoop {
    /** Keep the Worker and clients pointed at the same value. */
    public static final String TASK_QUEUE =
            envOr("TEMPORAL_TASK_QUEUE", "quickstart-human-in-the-loop");

    /**
     * A fixed Workflow ID, so approval commands can be written as fixed text. Only one Workflow
     * with this ID can run at a time.
     */
    public static final String WORKFLOW_ID =
            envOr("HUMAN_IN_THE_LOOP_WORKFLOW_ID", "human-in-the-loop");

    /** Name of the Signal that carries an {@link ApprovalDecision}. */
    public static final String APPROVAL_SIGNAL = "approval_decision";

    /** The namespace the client options connect to, for log lines. Unset means "default". */
    public static String namespaceOf(WorkflowClientOptions options) {
        String namespace = options.getNamespace();
        return namespace == null || namespace.isEmpty() ? "default" : namespace;
    }

    private static String envOr(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isEmpty() ? fallback : value;
    }

    private HumanInTheLoop() {}
}
