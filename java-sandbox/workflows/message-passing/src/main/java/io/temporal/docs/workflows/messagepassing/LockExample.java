package io.temporal.docs.workflows.messagepassing;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowLock;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;

public class LockExample {

    public static class Data {
        public String x;
        public String y;

        public Data() {}

        public Data(String x, String y) {
            this.x = x;
            this.y = y;
        }
    }

    @ActivityInterface
    public interface DataActivities {
        Data fetchData();
    }

    @WorkflowInterface
    public interface DataWorkflow {
        @WorkflowMethod
        void run();

        @SignalMethod
        void badSignalHandler();

        @SignalMethod
        void safeSignalHandler();

        @SignalMethod
        void finish();

        @QueryMethod
        String getXY();
    }

    public static class DataWorkflowImpl implements DataWorkflow {
        private final DataActivities activity = Workflow.newActivityStub(
            DataActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());
        private String x;
        private String y;
        private boolean finished;

        @Override
        public void run() {
            Workflow.await(() -> finished);
            Workflow.await(() -> Workflow.isEveryHandlerFinished());
        }

        // @@@SNIPSTART java-message-passing-bad-handler
        @Override
        public void badSignalHandler() {
            Data data = activity.fetchData();
            this.x = data.x;
            // Bug! If multiple instances of this method are executing concurrently, then
            // there may be times when the Workflow has this.x from one Activity execution and this.y from another.
            Workflow.sleep(Duration.ofSeconds(1));
            this.y = data.y;
        }
        // @@@SNIPEND

        // @@@SNIPSTART java-message-passing-safe-handler
        WorkflowLock lock = Workflow.newWorkflowLock();

        @Override
        public void safeSignalHandler() {
            lock.lock();
            try {
                Data data = activity.fetchData();
                this.x = data.x;
                // OK: the scheduler may switch now to a different handler execution,
                // or to the main workflow method, but no other execution of this handler
                // can run until this execution finishes.
                Workflow.sleep(Duration.ofSeconds(1));
                this.y = data.y;
            } finally {
                lock.unlock();
            }
        }
        // @@@SNIPEND

        @Override
        public void finish() {
            finished = true;
        }

        @Override
        public String getXY() {
            return x + y;
        }
    }
}
