package io.temporal.docs.workflows.messagepassing;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.docs.workflows.messagepassing.MessagePassingIntro.ApproveInput;
import io.temporal.docs.workflows.messagepassing.MessagePassingIntro.GetLanguagesInput;
import io.temporal.docs.workflows.messagepassing.MessagePassingIntro.GreetingWorkflow;
import io.temporal.docs.workflows.messagepassing.MessagePassingIntro.Language;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockingHandlerExample {

    @ActivityInterface
    public interface GreetingActivities {
        // Returns null when the greeting service doesn't support the language.
        String greetingService(Language language);
    }

    public static class GreetingWorkflowImpl implements GreetingWorkflow {
        private final Map<Language, String> greetings =
            new HashMap<>(Map.of(Language.ENGLISH, "Hello, world"));
        private Language language = Language.ENGLISH;
        private boolean approvedForRelease;

        private final GreetingActivities activity = Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

        @Override
        public String getGreetings() {
            Workflow.await(() -> approvedForRelease);
            return greetings.get(language);
        }

        @Override
        public List<Language> getLanguages(GetLanguagesInput input) {
            if (input.includeUnsupported) {
                return Arrays.asList(Language.values());
            }
            return new ArrayList<>(greetings.keySet());
        }

        @Override
        public void approve(ApproveInput input) {
            approvedForRelease = true;
        }

        // @@@SNIPSTART java-message-passing-blocking-handler
        @Override
        public Language setLanguage(Language language) {
            if (!greetings.containsKey(language)) {
                String greeting = activity.greetingService(language);
                if (greeting == null) {
                    // An update validator cannot be blocking, so cannot be used to check that the remote
                    // greetingService supports the requested language. Throwing an ApplicationFailure
                    // will fail the Update, but the WorkflowExecutionUpdateAccepted event will still be
                    // added to history.
                    throw ApplicationFailure.newFailure("Greeting service does not support: " + language, "GreetingFailure");
                }
                greetings.put(language, greeting);
            }
            Language previousLanguage = this.language;
            this.language = language;
            return previousLanguage;
        }
        // @@@SNIPEND

        @Override
        public void setLanguageValidator(Language language) {
            // Accept every language: setLanguage asks the greeting service instead.
        }
    }
}
