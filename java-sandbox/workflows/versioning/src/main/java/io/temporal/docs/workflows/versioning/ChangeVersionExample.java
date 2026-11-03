package io.temporal.docs.workflows.versioning;

import static io.temporal.docs.workflows.versioning.ChangeVersionSearchAttributes.TEMPORAL_CHANGE_VERSION;

import io.temporal.worker.Worker;
import io.temporal.worker.WorkflowImplementationOptions;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChangeVersionExample {

    @WorkflowInterface
    public interface YourWorkflow {
        @WorkflowMethod
        void run();
    }

    public static class YourWorkflowImpl implements YourWorkflow {
        @Override
        public void run() {
            Workflow.getVersion("checksumAdded", Workflow.DEFAULT_VERSION, 1);
        }
    }

    public static void registerWithAutoUpsert(Worker worker) {
        // @@@SNIPSTART java-versioning-auto-upsert
        WorkflowImplementationOptions options = WorkflowImplementationOptions.newBuilder()
            .setEnableUpsertVersionSearchAttributes(true)
            .build();

        worker.registerWorkflowImplementationTypes(options, YourWorkflowImpl.class);
        // @@@SNIPEND
    }

    @WorkflowInterface
    public interface ManualUpsertWorkflow {
        @WorkflowMethod
        void run();
    }

    public static class ManualUpsertWorkflowImpl implements ManualUpsertWorkflow {
        @Override
        public void run() {
            // @@@SNIPSTART java-versioning-manual-upsert
            int version = Workflow.getVersion("MovedThankYouAfterLoop", Workflow.DEFAULT_VERSION, 1);

            if (version != Workflow.DEFAULT_VERSION) {
              Workflow.upsertTypedSearchAttributes(TEMPORAL_CHANGE_VERSION
                  .valueSet(Arrays.asList("MovedThankYouAfterLoop-" + version)));
            }
            // @@@SNIPEND
        }
    }

    @WorkflowInterface
    public interface MultipleVersionsWorkflow {
        @WorkflowMethod
        void run();
    }

    public static class MultipleVersionsWorkflowImpl implements MultipleVersionsWorkflow {
        @Override
        public void run() {
            // @@@SNIPSTART java-versioning-manual-upsert-multiple
            List<String> list = new ArrayList<String>();
            int versionOne = Workflow.getVersion("versionOne", Workflow.DEFAULT_VERSION, 1);
            int versionTwo = Workflow.getVersion("versionTwo", Workflow.DEFAULT_VERSION, 1);
            if (versionOne != Workflow.DEFAULT_VERSION) {
               list.add("versionOne-" + versionOne);
            }
            if (versionTwo != Workflow.DEFAULT_VERSION) {
               list.add("versionTwo-" + versionTwo);
            }
            Workflow.upsertTypedSearchAttributes(TEMPORAL_CHANGE_VERSION.valueSet(list));
            // @@@SNIPEND
        }
    }
}
