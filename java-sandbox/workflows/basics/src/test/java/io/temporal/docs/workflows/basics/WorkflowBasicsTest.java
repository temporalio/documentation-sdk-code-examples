package io.temporal.docs.workflows.basics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.docs.workflows.basics.DevelopWorkflowExample.Arguments;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WorkflowBasicsTest {

    private static final String TASK_QUEUE = "workflow-basics";

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
            WorkflowOptions.newBuilder()
                .setTaskQueue(TASK_QUEUE)
                .setWorkflowId(workflowId)
                .build());
    }

    @Test
    public void greetingWorkflowReturnsGreeting() {
        worker.registerWorkflowImplementationTypes(DevelopWorkflowExample.GreetingWorkflowImpl.class);
        env.start();

        DevelopWorkflowExample.GreetingWorkflow workflow =
            stub(DevelopWorkflowExample.GreetingWorkflow.class, "greeting");
        assertEquals("Hello Temporal!", workflow.getGreeting("Temporal"));
    }

    @Test
    public void retryNowSignalsBothWorkflowsThroughBaseInterface() {
        worker.registerWorkflowImplementationTypes(
            InterfaceInheritanceExample.FileProcessingWorkflowImpl.class,
            InterfaceInheritanceExample.MediaProcessingWorkflowImpl.class);
        env.start();

        InterfaceInheritanceExample.FileProcessingWorkflow file =
            stub(InterfaceInheritanceExample.FileProcessingWorkflow.class, "file");
        InterfaceInheritanceExample.MediaProcessingWorkflow media =
            stub(InterfaceInheritanceExample.MediaProcessingWorkflow.class, "media");
        WorkflowClient.start(file::processFile, new Arguments("report.csv"));
        WorkflowClient.start(media::processBlob, new Arguments("video.mp4"));

        InterfaceInheritanceExample.signalRetryNow(client, "file", "media");

        assertEquals("processed report.csv", WorkflowStub.fromTyped(file).getResult(String.class));
        assertEquals("processed video.mp4", WorkflowStub.fromTyped(media).getResult(String.class));
    }

    @Test
    public void workflowMethodInBaseInterfaceGetsTypeFromEachSubinterface() {
        BaseWorkflowMethodExample.registerBoth(worker);
        env.start();

        BaseWorkflowMethodExample.Workflow1 workflow1 =
            stub(BaseWorkflowMethodExample.Workflow1.class, "workflow-1");
        BaseWorkflowMethodExample.Workflow2 workflow2 =
            stub(BaseWorkflowMethodExample.Workflow2.class, "workflow-2");
        workflow1.retryNow();
        workflow2.retryNow();

        assertEquals("Workflow1", WorkflowStub.fromTyped(workflow1).getWorkflowType().orElseThrow());
        assertEquals("Workflow2", WorkflowStub.fromTyped(workflow2).getWorkflowType().orElseThrow());
    }

    @Test
    public void customObjectParameter() {
        worker.registerWorkflowImplementationTypes(ParametersExample.YourWorkflowImpl.class);
        env.start();

        ParametersExample.YourWorkflow workflow = stub(ParametersExample.YourWorkflow.class, "params");
        assertEquals("order x2", workflow.yourWFMethod(new ParametersExample.CustomObj("order", 2)));
    }

    @Test
    public void workflowTypeNames() {
        worker.registerWorkflowImplementationTypes(
            DefaultWorkflowTypeExample.NotifyUserAccountsImpl.class,
            CustomWorkflowTypeExample.NotifyUserAccountsImpl.class);
        env.start();

        DefaultWorkflowTypeExample.NotifyUserAccounts defaultType =
            stub(DefaultWorkflowTypeExample.NotifyUserAccounts.class, "default-type");
        defaultType.notify(new String[] {"a"});
        assertEquals("NotifyUserAccounts",
            WorkflowStub.fromTyped(defaultType).getWorkflowType().orElseThrow());

        CustomWorkflowTypeExample.NotifyUserAccounts customType =
            stub(CustomWorkflowTypeExample.NotifyUserAccounts.class, "custom-type");
        customType.notify(new String[] {"a"});
        assertEquals("your-workflow",
            WorkflowStub.fromTyped(customType).getWorkflowType().orElseThrow());
    }

    @Test
    public void workflowInitConstructorReceivesInput() {
        worker.registerWorkflowImplementationTypes(GreetingExample.GreetingWorkflowImpl.class);
        worker.registerActivitiesImplementations(
            (GreetingExample.TitleActivities) nameWithTitle -> nameWithTitle.startsWith("Knight "));
        env.start();

        GreetingExample.GreetingWorkflow workflow =
            stub(GreetingExample.GreetingWorkflow.class, "workflow-init");
        WorkflowClient.start(workflow::getGreeting, "Temporal");
        assertTrue(workflow.checkTitleValidity());
        assertEquals("Hello Knight Temporal", WorkflowStub.fromTyped(workflow).getResult(String.class));
    }

    @Test
    public void loggingWorkflow() {
        worker.registerWorkflowImplementationTypes(LoggingExample.MyWorkflowImpl.class);
        env.start();

        LoggingExample.MyWorkflow workflow = stub(LoggingExample.MyWorkflow.class, "logging");
        assertEquals("Hello Temporal", workflow.execute("Temporal"));
    }

    @Test
    public void deterministicApis() {
        worker.registerWorkflowImplementationTypes(
            DeterministicApisExample.DeterministicApisWorkflowImpl.class);
        env.start();

        DeterministicApisExample.DeterministicApisWorkflow workflow =
            stub(DeterministicApisExample.DeterministicApisWorkflow.class, "deterministic");
        assertEquals(3, workflow.run().split(" ").length);
    }
}
