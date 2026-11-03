package io.temporal.docs.workflows.timers;

import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;

public class TimerExample {

    @WorkflowInterface
    public interface TimerWorkflow {
        @WorkflowMethod
        String run();
    }

    public static class TimerWorkflowImpl implements TimerWorkflow {
        @Override
        public String run() {
            // @@@SNIPSTART java-timers-sleep
            Workflow.sleep(Duration.ofSeconds(5));
            // @@@SNIPEND
            return "Timer fired";
        }
    }
}
