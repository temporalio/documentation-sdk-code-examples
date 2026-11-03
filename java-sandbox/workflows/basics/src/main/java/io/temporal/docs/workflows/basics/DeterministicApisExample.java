package io.temporal.docs.workflows.basics;

import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import io.temporal.workflow.unsafe.WorkflowUnsafe;
import java.util.UUID;

public class DeterministicApisExample {

    @WorkflowInterface
    public interface DeterministicApisWorkflow {
        @WorkflowMethod
        String run();
    }

    public static class DeterministicApisWorkflowImpl implements DeterministicApisWorkflow {

        @Override
        public String run() {
            // @@@SNIPSTART java-workflow-basics-random
            int value = Workflow.newRandom().nextInt(100);
            UUID uniqueId = Workflow.randomUUID();
            // @@@SNIPEND

            // @@@SNIPSTART java-workflow-basics-current-time
            long currentTime = Workflow.currentTimeMillis();
            // @@@SNIPEND

            // @@@SNIPSTART java-workflow-basics-is-replaying
            if (!WorkflowUnsafe.isReplaying()) {
              emitMetric("workflow_started", 1);
            }
            // @@@SNIPEND

            return value + " " + uniqueId + " " + currentTime;
        }

        private void emitMetric(String name, long value) {
            Workflow.getMetricsScope().counter(name).inc(value);
        }
    }
}
