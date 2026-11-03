package io.temporal.docs.workflows.basics;

import io.temporal.client.WorkflowClient;
import io.temporal.docs.workflows.basics.DevelopWorkflowExample.Arguments;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.ArrayList;
import java.util.List;

public class InterfaceInheritanceExample {

    // @@@SNIPSTART java-workflow-basics-inheritance-retryable
    public interface Retryable {
        @SignalMethod
        void retryNow();
    }

    @WorkflowInterface
    public interface FileProcessingWorkflow extends Retryable {

        @WorkflowMethod
        String processFile(Arguments args);

        @QueryMethod(name="history")
        List<String> getHistory();

        @QueryMethod
        String getStatus();

        @SignalMethod
        void abandon();
    }
    // @@@SNIPEND

    // @@@SNIPSTART java-workflow-basics-inheritance-media
    @WorkflowInterface
    public interface MediaProcessingWorkflow extends Retryable {

        @WorkflowMethod
        String processBlob(Arguments args);
    }
    // @@@SNIPEND

    public static class FileProcessingWorkflowImpl implements FileProcessingWorkflow {
        private final List<String> history = new ArrayList<>();
        private String status = "waiting";
        private boolean retried;
        private boolean abandoned;

        @Override
        public String processFile(Arguments args) {
            history.add("started " + args.name);
            Workflow.await(() -> retried || abandoned);
            status = abandoned ? "abandoned" : "processed";
            history.add(status);
            return status + " " + args.name;
        }

        @Override
        public List<String> getHistory() {
            return history;
        }

        @Override
        public String getStatus() {
            return status;
        }

        @Override
        public void abandon() {
            abandoned = true;
        }

        @Override
        public void retryNow() {
            history.add("retryNow");
            retried = true;
        }
    }

    public static class MediaProcessingWorkflowImpl implements MediaProcessingWorkflow {
        private boolean retried;

        @Override
        public String processBlob(Arguments args) {
            Workflow.await(() -> retried);
            return "processed " + args.name;
        }

        @Override
        public void retryNow() {
            retried = true;
        }
    }

    public static void signalRetryNow(
            WorkflowClient client, String firstWorkflowId, String secondWorkflowId) {
        // @@@SNIPSTART java-workflow-basics-inheritance-signal
        Retryable r1 = client.newWorkflowStub(Retryable.class, firstWorkflowId);
        Retryable r2 = client.newWorkflowStub(Retryable.class, secondWorkflowId);
        r1.retryNow();
        r2.retryNow();
        // @@@SNIPEND
    }
}
