package io.temporal.docs.workflows.basics;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

public class CustomWorkflowTypeExample {

    // @@@SNIPSTART java-workflow-basics-type-custom
    @WorkflowInterface
    public interface NotifyUserAccounts {
        @WorkflowMethod(name = "your-workflow")
        void notify(String[] accountIds);
    }
    // @@@SNIPEND

    public static class NotifyUserAccountsImpl implements NotifyUserAccounts {
        @Override
        public void notify(String[] accountIds) {}
    }
}
