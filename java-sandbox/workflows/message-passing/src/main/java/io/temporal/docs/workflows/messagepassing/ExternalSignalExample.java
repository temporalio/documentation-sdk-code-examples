package io.temporal.docs.workflows.messagepassing;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

public class ExternalSignalExample {

    @WorkflowInterface
    public interface OtherWorkflow {
        @WorkflowMethod
        String run();

        @SignalMethod
        void mySignalMethod();
    }

    public static class OtherWorkflowImpl implements OtherWorkflow {
        private boolean signaled;

        @Override
        public String run() {
            Workflow.await(() -> signaled);
            return "signaled";
        }

        @Override
        public void mySignalMethod() {
            signaled = true;
        }
    }

    @WorkflowInterface
    public interface SenderWorkflow {
        @WorkflowMethod
        void signalOther(String otherWorkflowID);
    }

    public static class SenderWorkflowImpl implements SenderWorkflow {
        @Override
        public void signalOther(String otherWorkflowID) {
            // @@@SNIPSTART java-message-passing-external-signal
            OtherWorkflow other = Workflow.newExternalWorkflowStub(OtherWorkflow.class, otherWorkflowID);
            other.mySignalMethod();
            // @@@SNIPEND
        }
    }
}
