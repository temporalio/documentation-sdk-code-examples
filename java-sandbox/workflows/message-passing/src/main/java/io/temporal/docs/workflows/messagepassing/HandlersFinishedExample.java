package io.temporal.docs.workflows.messagepassing;

import io.temporal.workflow.HandlerUnfinishedPolicy;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;

public class HandlersFinishedExample {

    // @@@SNIPSTART java-message-passing-unfinished-policy
    @WorkflowInterface
    public interface MyWorkflow {
        @WorkflowMethod
        String run();

        @SignalMethod
        void finish();

        @UpdateMethod(unfinishedPolicy = HandlerUnfinishedPolicy.ABANDON)
        void myUpdate();
    }
    // @@@SNIPEND

    public static class MyWorkflowImpl implements MyWorkflow {
        private boolean finished;
        private int updatesCompleted;

        // @@@SNIPSTART java-message-passing-wait-for-handlers
        @Override
        public String run() {
            Workflow.await(() -> finished);
            Workflow.await(() -> Workflow.isEveryHandlerFinished());
            return "workflow-result";
        }
        // @@@SNIPEND

        @Override
        public void finish() {
            finished = true;
        }

        @Override
        public void myUpdate() {
            Workflow.sleep(Duration.ofSeconds(5));
            updatesCompleted++;
        }
    }
}
