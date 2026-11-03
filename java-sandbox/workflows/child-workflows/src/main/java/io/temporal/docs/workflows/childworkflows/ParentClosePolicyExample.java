package io.temporal.docs.workflows.childworkflows;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.ParentClosePolicy;
import io.temporal.workflow.Async;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Promise;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

public class ParentClosePolicyExample {

    @WorkflowInterface
    public interface ParentWorkflow {
        @WorkflowMethod
        void parentWorkflow();
    }

    @WorkflowInterface
    public interface MyChildWorkflow {
        @WorkflowMethod
        void processOrder(String orderId);

        @SignalMethod
        void finish();
    }

    public static class MyChildWorkflowImpl implements MyChildWorkflow {
        private boolean finished;

        @Override
        public void processOrder(String orderId) {
            Workflow.await(() -> finished);
        }

        @Override
        public void finish() {
            finished = true;
        }
    }

    public static class ParentWorkflowImpl implements ParentWorkflow {
        // @@@SNIPSTART java-child-workflows-parent-close-policy
        public void parentWorkflow() {
            ChildWorkflowOptions options =
                ChildWorkflowOptions.newBuilder()
                    .setParentClosePolicy(ParentClosePolicy.PARENT_CLOSE_POLICY_ABANDON)
                    .build();
            MyChildWorkflow child = Workflow.newChildWorkflowStub(MyChildWorkflow.class, options);
            Async.procedure(child::processOrder, "order-123");
            Promise<WorkflowExecution> childExecution = Workflow.getWorkflowExecution(child);
            // Wait for child to start
            childExecution.get();
        }
        // @@@SNIPEND
    }
}
