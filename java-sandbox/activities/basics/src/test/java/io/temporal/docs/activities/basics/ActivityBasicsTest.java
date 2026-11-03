package io.temporal.docs.activities.basics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.temporal.activity.ActivityOptions;
import io.temporal.api.enums.v1.EventType;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ActivityBasicsTest {

    private static final String TASK_QUEUE = "activity-basics";
    private static final ActivityOptions OPTIONS =
        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build();

    @WorkflowInterface
    public interface CallerWorkflow {
        @WorkflowMethod
        String run(String example);
    }

    /** Calls every method of the example's interface, so the history records each Activity Type. */
    public static class CallerWorkflowImpl implements CallerWorkflow {
        @Override
        public String run(String example) {
            switch (example) {
                case "define":
                    return Workflow.newActivityStub(DefineActivityExample.GreetingActivities.class, OPTIONS)
                        .composeGreeting("Hello", "World");
                case "parameters": {
                    ActivityParametersExample.YourActivities a =
                        Workflow.newActivityStub(ActivityParametersExample.YourActivities.class, OPTIONS);
                    a.sendCustomObject(new ActivityParametersExample.CustomObj("x"), "abc");
                    return a.getCustomObject(new ActivityParametersExample.CustomObj("custom"));
                }
                case "default": {
                    ActivityTypeExample.DefaultName.GreetingActivities a =
                        Workflow.newActivityStub(ActivityTypeExample.DefaultName.GreetingActivities.class, OPTIONS);
                    return a.sendMessage("m") + a.composeGreeting("g", "l");
                }
                case "prefix": {
                    ActivityTypeExample.Prefix.GreetingActivities a =
                        Workflow.newActivityStub(ActivityTypeExample.Prefix.GreetingActivities.class, OPTIONS);
                    return a.sendMessage("m") + a.composeGreeting("g", "l");
                }
                default: {
                    ActivityTypeExample.CustomName.GreetingActivities a =
                        Workflow.newActivityStub(ActivityTypeExample.CustomName.GreetingActivities.class, OPTIONS);
                    return a.sendMessage("m") + a.composeGreeting("g", "l") + a.composeFarewell("f", "l");
                }
            }
        }
    }

    static class DefaultNameImpl implements ActivityTypeExample.DefaultName.GreetingActivities {
        public String sendMessage(String input) { return "1"; }
        public String composeGreeting(String greeting, String language) { return "2"; }
    }

    static class PrefixImpl implements ActivityTypeExample.Prefix.GreetingActivities {
        public String sendMessage(String input) { return "1"; }
        public String composeGreeting(String greeting, String language) { return "2"; }
    }

    static class CustomNameImpl implements ActivityTypeExample.CustomName.GreetingActivities {
        public String sendMessage(String input) { return "1"; }
        public String composeGreeting(String greeting, String language) { return "2"; }
        public String composeFarewell(String farewell, String language) { return "3"; }
    }

    private TestWorkflowEnvironment env;
    private Worker worker;

    @BeforeEach
    public void setUp() {
        env = TestWorkflowEnvironment.newInstance();
        worker = env.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(CallerWorkflowImpl.class);
    }

    @AfterEach
    public void tearDown() {
        env.close();
    }

    private List<String> run(String example, Object activities, String expectedResult) {
        worker.registerActivitiesImplementations(activities);
        env.start();
        String workflowId = "caller-" + example;
        String result = env.getWorkflowClient().newWorkflowStub(CallerWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(workflowId).build())
            .run(example);
        assertEquals(expectedResult, result);
        return env.getWorkflowClient().fetchHistory(workflowId).getEvents().stream()
            .filter(e -> e.getEventType() == EventType.EVENT_TYPE_ACTIVITY_TASK_SCHEDULED)
            .map(e -> e.getActivityTaskScheduledEventAttributes().getActivityType().getName())
            .collect(Collectors.toList());
    }

    @Test
    public void implementationComposesGreeting() {
        assertEquals(List.of("ComposeGreeting"),
            run("define", DefineActivityExample.newImplementation(), "Hello World!"));
    }

    @Test
    public void customObjectParameters() {
        assertEquals(List.of("SendCustomObject", "GetCustomObject"),
            run("parameters", new ActivityParametersExample.YourActivitiesImpl(), "custom"));
    }

    @Test
    public void defaultActivityTypes() {
        assertEquals(List.of("SendMessage", "ComposeGreeting"), run("default", new DefaultNameImpl(), "12"));
    }

    @Test
    public void prefixedActivityTypes() {
        assertEquals(List.of("Messaging_SendMessage", "Messaging_ComposeGreeting"),
            run("prefix", new PrefixImpl(), "12"));
    }

    @Test
    public void customActivityTypeName() {
        assertEquals(List.of("Messaging_SendMessage", "Messaging_ComposeGreeting", "farewell"),
            run("custom", new CustomNameImpl(), "123"));
    }
}
