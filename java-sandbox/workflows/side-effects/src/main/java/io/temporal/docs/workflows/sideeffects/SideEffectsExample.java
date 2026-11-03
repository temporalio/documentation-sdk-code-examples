package io.temporal.docs.workflows.sideeffects;

import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.security.SecureRandom;
import java.util.Random;

public class SideEffectsExample {

    @WorkflowInterface
    public interface SideEffectWorkflow {
        @WorkflowMethod
        void execute();
    }

    public static class SideEffectWorkflowImpl implements SideEffectWorkflow {
        @Override
        public void execute() {
            // @@@SNIPSTART java-side-effects-basic
            int randomValue = Workflow.sideEffect(Integer.class, () -> new Random().nextInt(100));
            if (randomValue < 50) {
                // ...
            } else {
                // ...
            }
            // @@@SNIPEND
        }
    }

    @WorkflowInterface
    public interface SecureRandomWorkflow {
        @WorkflowMethod
        void execute();
    }

    public static class SecureRandomWorkflowImpl implements SecureRandomWorkflow {
        // @@@SNIPSTART java-side-effects-secure-random
        // implementation of the @WorkflowMethod
        public void execute() {
            int randomInt = Workflow.sideEffect(int.class, () -> {
                Random random = new SecureRandom();
                return random.nextInt();
            });

            String userHome = Workflow.sideEffect(String.class, () -> System.getenv("USER_HOME"));

            if (randomInt % 2 == 0) {
                // ...
            } else {
                // ...
            }
        }
        // @@@SNIPEND
    }

    @WorkflowInterface
    public interface NewRandomWorkflow {
        @WorkflowMethod
        void execute();
    }

    public static class NewRandomWorkflowImpl implements NewRandomWorkflow {
        // @@@SNIPSTART java-side-effects-new-random
        // implementation of the @WorkflowMethod
        public void execute() {
            int randomInt = Workflow.newRandom().nextInt();
            // ...
        }
        // @@@SNIPEND
    }

    @WorkflowInterface
    public interface RandomUuidWorkflow {
        @WorkflowMethod
        void execute();
    }

    public static class RandomUuidWorkflowImpl implements RandomUuidWorkflow {
        // @@@SNIPSTART java-side-effects-random-uuid
        // implementation of the @WorkflowMethod
        public void execute() {
            String randomUUID = Workflow.randomUUID().toString();
            // ...
        }
        // @@@SNIPEND
    }
}
