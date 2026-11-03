package io.temporal.docs.workflows.messagepassing;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

public class WaitInHandlerExample {

    public static class UpdateInput {
        public String language;

        public UpdateInput() {}

        public UpdateInput(String language) {
            this.language = language;
        }
    }

    @WorkflowInterface
    public interface LanguageWorkflow {
        @WorkflowMethod
        String run();

        @SignalMethod
        void enableUpdates();

        @UpdateMethod
        String setLanguage(UpdateInput input);
    }

    public static class LanguageWorkflowImpl implements LanguageWorkflow {
        private boolean updatesEnabled;
        private String language = "English";

        @Override
        public String run() {
            Workflow.await(() -> !"English".equals(language));
            Workflow.await(() -> Workflow.isEveryHandlerFinished());
            return language;
        }

        @Override
        public void enableUpdates() {
            updatesEnabled = true;
        }

        private boolean updateReadyToExecute(UpdateInput input) {
            return updatesEnabled;
        }

        // @@@SNIPSTART java-message-passing-wait-in-handler
        @Override
        public String setLanguage(UpdateInput input) {
            Workflow.await(() -> this.updateReadyToExecute(input));
            String previousLanguage = this.language;
            this.language = input.language;
            return previousLanguage;
        }
        // @@@SNIPEND
    }
}
