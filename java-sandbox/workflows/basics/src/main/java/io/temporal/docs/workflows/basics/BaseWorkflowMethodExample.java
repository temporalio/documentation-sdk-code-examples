package io.temporal.docs.workflows.basics;

import io.temporal.worker.Worker;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

public class BaseWorkflowMethodExample {

    // @@@SNIPSTART java-workflow-basics-base-workflow-method
    public interface BaseWorkflow {
        @WorkflowMethod
        void retryNow();
    }

    @WorkflowInterface
    public interface Workflow1 extends BaseWorkflow {}

    @WorkflowInterface
    public interface Workflow2 extends BaseWorkflow {}
    // @@@SNIPEND

    public static class Workflow1Impl implements Workflow1 {
        @Override
        public void retryNow() {}
    }

    public static class Workflow2Impl implements Workflow2 {
        @Override
        public void retryNow() {}
    }

    public static void registerBoth(Worker worker) {
        // @@@SNIPSTART java-workflow-basics-base-workflow-method-registration
        worker.registerWorkflowImplementationTypes(
            Workflow1Impl.class, Workflow2Impl.class);
        // @@@SNIPEND
    }
}
