package io.temporal.docs.workflows.basics;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

public class DefaultWorkflowTypeExample {

    // @@@SNIPSTART java-workflow-basics-type-default
    @WorkflowInterface
    public interface NotifyUserAccounts {
        @WorkflowMethod
        void notify(String[] accountIds);
    }
    // @@@SNIPEND

    public static class NotifyUserAccountsImpl implements NotifyUserAccounts {
        @Override
        public void notify(String[] accountIds) {}
    }
}
