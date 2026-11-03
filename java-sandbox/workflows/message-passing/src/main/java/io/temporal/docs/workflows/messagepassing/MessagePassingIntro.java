package io.temporal.docs.workflows.messagepassing;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.UpdateValidatorMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// @@@SNIPSTART java-message-passing-intro
public class MessagePassingIntro {

    public enum Language {
        CHINESE,
        ENGLISH,
        FRENCH,
        SPANISH,
        PORTUGUESE,
    }

    public static class GetLanguagesInput {
        public boolean includeUnsupported;

        public GetLanguagesInput() {
            this.includeUnsupported = false;
        }

        public GetLanguagesInput(boolean includeUnsupported) {
            this.includeUnsupported = includeUnsupported;
        }
    }

    public static class ApproveInput {
        public String name;

        public ApproveInput() {}

        public ApproveInput(String name) {
            this.name = name;
        }
    }

    @WorkflowInterface
    public interface GreetingWorkflow {
        @WorkflowMethod
        String getGreetings();

        // Use the @QueryMethod annotation to define a Query handler in the
        // Workflow interface.
        @QueryMethod
        List<Language> getLanguages(GetLanguagesInput input);

        // Use the @SignalMethod annotation to define a Signal handler in the
        // Workflow interface.
        @SignalMethod
        void approve(ApproveInput input);

        // Use the @UpdateMethod annotation to define an Update handler in the
        // Workflow interface.
        @UpdateMethod
        Language setLanguage(Language language);

        // Update validators are optional
        @UpdateValidatorMethod(updateName = "setLanguage")
        void setLanguageValidator(Language language);
    }

    public static class GreetingWorkflowImpl implements GreetingWorkflow {
        private final Map<Language, String> greetings =
            new HashMap<>(Map.of(Language.ENGLISH, "Hello, world", Language.CHINESE, "你好，世界"));
        private Language language = Language.ENGLISH;
        private boolean approvedForRelease;
        private String approverName;

        @Override
        public String getGreetings() {
            Workflow.await(() -> approvedForRelease);
            return greetings.get(language) + " (approved by " + approverName + ")";
        }

        @Override
        public List<Language> getLanguages(GetLanguagesInput input) {
            // The Query handler returns a value: it must not mutate the Workflow state
            // or perform blocking operations.
            if (input.includeUnsupported) {
                return Arrays.asList(Language.values());
            } else {
                return new ArrayList<>(greetings.keySet());
            }
        }

        @Override
        public void approve(ApproveInput input) {
            // The Signal handler mutates the Workflow state but cannot return a value.
            this.approvedForRelease = true;
            this.approverName = input.name;
        }

        @Override
        public Language setLanguage(Language language) {
            // The Update handler can mutate the Workflow state and return a value.
            Language previousLanguage = this.language;
            this.language = language;
            return previousLanguage;
        }

        @Override
        public void setLanguageValidator(Language language) {
            // The Update validator performs validation but cannot mutate the Workflow state.
            if (!greetings.containsKey(language)) {
                throw new IllegalArgumentException("Unsupported language: " + language);
            }
        }
    }
}
// @@@SNIPEND
