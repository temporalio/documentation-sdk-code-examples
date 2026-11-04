package io.temporal.docs.workers.runprocess;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.worker.Worker;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RegisterTypesExample {

    /** Stands in for a thread-safe database client shared by every Activity call. */
    public static class DatabaseClient {
        private final Map<String, String> greetings = new ConcurrentHashMap<>();

        public DatabaseClient() {
            greetings.put("en", "Hello");
        }

        public String findGreeting(String language) {
            return greetings.getOrDefault(language, "Hello");
        }
    }

    @ActivityInterface
    public interface GreetingActivities {
        String composeGreeting(String language, String name);
    }

    public static class GreetingActivitiesImpl implements GreetingActivities {
        private final DatabaseClient databaseClient;

        public GreetingActivitiesImpl() {
            this(new DatabaseClient());
        }

        public GreetingActivitiesImpl(DatabaseClient databaseClient) {
            this.databaseClient = databaseClient;
        }

        @Override
        public String composeGreeting(String language, String name) {
            return databaseClient.findGreeting(language) + " " + name + "!";
        }
    }

    @WorkflowInterface
    public interface GreetingWorkflow {
        @WorkflowMethod
        String greet(String name);
    }

    public static class GreetingWorkflowImpl implements GreetingWorkflow {
        private final GreetingActivities activities =
            Workflow.newActivityStub(
                GreetingActivities.class,
                ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .build());

        @Override
        public String greet(String name) {
            return activities.composeGreeting("en", name);
        }
    }

    @WorkflowInterface
    public interface OrderWorkflow {
        @WorkflowMethod
        String placeOrder(String orderId);
    }

    public static class OrderWorkflowImpl implements OrderWorkflow {
        @Override
        public String placeOrder(String orderId) {
            return "Placed order " + orderId;
        }
    }

    public static void registerTypes(Worker worker, DatabaseClient databaseClient) {
        // @@@SNIPSTART java-run-worker-register-types
        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class, OrderWorkflowImpl.class);
        worker.registerActivitiesImplementations(new GreetingActivitiesImpl(databaseClient));
        // @@@SNIPEND
    }
}
