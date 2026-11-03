package io.temporal.docs.workflows.messagepassing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.temporal.activity.ActivityOptions;
import io.temporal.api.enums.v1.WorkflowIdConflictPolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.client.WorkflowUpdateException;
import io.temporal.client.WorkflowUpdateStage;
import io.temporal.docs.workflows.messagepassing.MessagePassingIntro.ApproveInput;
import io.temporal.docs.workflows.messagepassing.MessagePassingIntro.GetLanguagesInput;
import io.temporal.docs.workflows.messagepassing.MessagePassingIntro.GreetingWorkflow;
import io.temporal.docs.workflows.messagepassing.MessagePassingIntro.Language;
import io.temporal.failure.ApplicationFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MessagePassingTest {

    private static final String TASK_QUEUE = MessagePassingStarter.TASK_QUEUE;

    private TestWorkflowEnvironment env;
    private Worker worker;
    private WorkflowClient client;

    @BeforeEach
    public void setUp() {
        env = TestWorkflowEnvironment.newInstance();
        worker = env.newWorker(TASK_QUEUE);
        client = env.getWorkflowClient();
    }

    @AfterEach
    public void tearDown() {
        env.close();
    }

    private <T> T stub(Class<T> type, String workflowId) {
        return client.newWorkflowStub(
            type,
            WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(workflowId).build());
    }

    @Test
    public void queriesSignalsAndUpdates() throws Exception {
        worker.registerWorkflowImplementationTypes(MessagePassingIntro.GreetingWorkflowImpl.class);
        env.start();

        GreetingWorkflow workflow = stub(GreetingWorkflow.class, "intro");
        WorkflowClient.start(workflow::getGreetings);

        assertEquals(5, workflow.getLanguages(new GetLanguagesInput(true)).size());
        List<Language> supported = workflow.getLanguages(new GetLanguagesInput(false));
        assertEquals(2, supported.size());

        MessagePassingStarter.sendMessages(workflow);
        assertEquals("Hello, world (approved by Me)",
            WorkflowStub.fromTyped(workflow).getResult(String.class));
    }

    @Test
    public void validatorRejectsUnsupportedLanguage() {
        worker.registerWorkflowImplementationTypes(MessagePassingIntro.GreetingWorkflowImpl.class);
        env.start();

        GreetingWorkflow workflow = stub(GreetingWorkflow.class, "validator");
        WorkflowClient.start(workflow::getGreetings);

        assertThrows(WorkflowUpdateException.class, () -> workflow.setLanguage(Language.FRENCH));
        workflow.approve(new ApproveInput("Me"));
    }

    @Test
    public void updateWithStart() throws Exception {
        worker.registerWorkflowImplementationTypes(MessagePassingIntro.GreetingWorkflowImpl.class);
        env.start();

        WorkflowOptions options = WorkflowOptions.newBuilder()
            .setTaskQueue(TASK_QUEUE)
            .setWorkflowId("update-with-start")
            .setWorkflowIdConflictPolicy(WorkflowIdConflictPolicy.WORKFLOW_ID_CONFLICT_POLICY_USE_EXISTING)
            .build();
        assertEquals(Language.ENGLISH, MessagePassingStarter.startUpdateWithStart(
            client.newWorkflowStub(GreetingWorkflow.class, options)));
        // The second call finds the running Workflow and sends it the Update.
        assertEquals(Language.ENGLISH, MessagePassingStarter.executeUpdateWithStart(
            client.newWorkflowStub(GreetingWorkflow.class, options)));
        client.newWorkflowStub(GreetingWorkflow.class, "update-with-start").approve(new ApproveInput("Me"));
    }

    @Test
    public void signalHandlerRunsBeforeWorkflowMethodWithSignalWithStart() {
        worker.registerWorkflowImplementationTypes(SignalWithStartExample.GreetingWorkflowImpl.class);
        env.start();

        String greeting = SignalWithStartExample.signalWithStart(
            client, "signal-with-start", TASK_QUEUE,
            new SignalWithStartExample.Customer("customer1"),
            new SignalWithStartExample.Customer("customer2"));
        assertEquals("Hello customer2", greeting);
    }

    @Test
    public void externalSignal() {
        worker.registerWorkflowImplementationTypes(
            ExternalSignalExample.OtherWorkflowImpl.class, ExternalSignalExample.SenderWorkflowImpl.class);
        env.start();

        ExternalSignalExample.OtherWorkflow other = stub(ExternalSignalExample.OtherWorkflow.class, "other");
        WorkflowClient.start(other::run);
        stub(ExternalSignalExample.SenderWorkflow.class, "sender").signalOther("other");
        assertEquals("signaled", WorkflowStub.fromTyped(other).getResult(String.class));
    }

    @Test
    public void blockingUpdateHandlerCallsActivity() {
        worker.registerWorkflowImplementationTypes(BlockingHandlerExample.GreetingWorkflowImpl.class);
        worker.registerActivitiesImplementations(
            (BlockingHandlerExample.GreetingActivities) language ->
                language == Language.FRENCH ? "Bonjour, monde" : null);
        env.start();

        GreetingWorkflow workflow = stub(GreetingWorkflow.class, "blocking");
        WorkflowClient.start(workflow::getGreetings);

        assertEquals(Language.ENGLISH, workflow.setLanguage(Language.FRENCH));
        WorkflowUpdateException e =
            assertThrows(WorkflowUpdateException.class, () -> workflow.setLanguage(Language.SPANISH));
        assertTrue(e.getCause() instanceof ApplicationFailure);

        workflow.approve(new ApproveInput("Me"));
        assertEquals("Bonjour, monde", WorkflowStub.fromTyped(workflow).getResult(String.class));
    }

    @Test
    public void updateWaitsForCondition() throws Exception {
        worker.registerWorkflowImplementationTypes(WaitInHandlerExample.LanguageWorkflowImpl.class);
        env.start();

        WaitInHandlerExample.LanguageWorkflow workflow =
            stub(WaitInHandlerExample.LanguageWorkflow.class, "wait-in-handler");
        WorkflowClient.start(workflow::run);

        CompletableFuture<String> update = WorkflowStub.fromTyped(workflow)
            .startUpdate("setLanguage", WorkflowUpdateStage.ACCEPTED, String.class,
                new WaitInHandlerExample.UpdateInput("French"))
            .getResultAsync();
        assertTrue(!update.isDone());

        workflow.enableUpdates();
        assertEquals("English", update.get());
        assertEquals("French", WorkflowStub.fromTyped(workflow).getResult(String.class));
    }

    @Test
    public void workflowWaitsForHandlersToFinish() throws Exception {
        worker.registerWorkflowImplementationTypes(HandlersFinishedExample.MyWorkflowImpl.class);
        env.start();

        HandlersFinishedExample.MyWorkflow workflow =
            stub(HandlersFinishedExample.MyWorkflow.class, "handlers-finished");
        WorkflowClient.start(workflow::run);

        CompletableFuture<Void> update = WorkflowStub.fromTyped(workflow)
            .startUpdate("myUpdate", WorkflowUpdateStage.ACCEPTED, Void.class)
            .getResultAsync();
        workflow.finish();

        assertEquals("workflow-result", WorkflowStub.fromTyped(workflow).getResult(String.class));
        assertTrue(update.isDone());
    }

    @Test
    public void lockKeepsHandlerStateConsistent() {
        worker.registerWorkflowImplementationTypes(LockExample.DataWorkflowImpl.class);
        int[] calls = {0};
        worker.registerActivitiesImplementations((LockExample.DataActivities) () -> {
            calls[0]++;
            return new LockExample.Data("x" + calls[0], "y" + calls[0]);
        });
        env.start();

        LockExample.DataWorkflow workflow = stub(LockExample.DataWorkflow.class, "lock");
        WorkflowClient.start(workflow::run);
        workflow.safeSignalHandler();
        workflow.safeSignalHandler();
        workflow.badSignalHandler();
        workflow.finish();
        WorkflowStub.fromTyped(workflow).getResult(Void.class);
        assertEquals(3, calls[0]);
    }

    @Test
    public void dynamicWorkflowAndActivity() {
        worker.registerWorkflowImplementationTypes(DynamicExample.MyDynamicWorkflow.class);
        worker.registerActivitiesImplementations(new DynamicExample.DynamicGreetingActivityImpl());
        env.start();

        WorkflowStub workflow = client.newUntypedWorkflowStub("AnyWorkflowType",
            WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId("dynamic").build());
        workflow.start("Temporal");
        assertEquals("AnyWorkflowType: Temporal", workflow.getResult(String.class));
    }

    @Test
    public void dynamicActivityImplementsAnyActivityType() {
        worker.registerWorkflowImplementationTypes(CallsDynamicActivityWorkflowImpl.class);
        worker.registerActivitiesImplementations(new DynamicExample.DynamicGreetingActivityImpl());
        env.start();

        assertEquals("composeGreeting: Hello World from: Temporal",
            stub(CallsDynamicActivityWorkflow.class, "dynamic-activity").run());
    }

    @Test
    public void dynamicSignalQueryAndUpdate() {
        worker.registerWorkflowImplementationTypes(DynamicExample.DynamicHandlersWorkflow.class);
        env.start();

        WorkflowStub workflow = client.newUntypedWorkflowStub("AnyWorkflowType",
            WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId("dynamic-handlers").build());
        workflow.start();
        workflow.signal("anySignal", "Temporal");
        assertEquals("Temporal", workflow.query("anyQuery", String.class));
        assertEquals("echo", workflow.update("anyUpdate", String.class, "echo"));
        workflow.signal("anySignal", "done");
        assertEquals("done", workflow.getResult(String.class));
    }

    @io.temporal.workflow.WorkflowInterface
    public interface CallsDynamicActivityWorkflow {
        @io.temporal.workflow.WorkflowMethod
        String run();
    }

    public static class CallsDynamicActivityWorkflowImpl implements CallsDynamicActivityWorkflow {
        @Override
        public String run() {
            return io.temporal.workflow.Workflow.newUntypedActivityStub(
                    ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build())
                .execute("composeGreeting", String.class, "Hello", "World", "Temporal");
        }
    }
}
