package io.temporal.docs.workflows.messagepassing;

import io.temporal.client.UpdateOptions;
import io.temporal.client.WithStartWorkflowOperation;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.client.WorkflowUpdateHandle;
import io.temporal.client.WorkflowUpdateStage;
import io.temporal.docs.workflows.messagepassing.MessagePassingIntro.ApproveInput;
import io.temporal.docs.workflows.messagepassing.MessagePassingIntro.GetLanguagesInput;
import io.temporal.docs.workflows.messagepassing.MessagePassingIntro.GreetingWorkflow;
import io.temporal.docs.workflows.messagepassing.MessagePassingIntro.Language;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MessagePassingStarter {

    static final String TASK_QUEUE = "message-passing";
    static final String WORKFLOW_ID = "message-passing-intro";

    public static void main(String[] args) throws Exception {
        // @@@SNIPSTART java-message-passing-stub
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

        WorkflowClient client = WorkflowClient.newInstance(service);

        WorkflowOptions workflowOptions =
            WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(WORKFLOW_ID).build();

        // Create the workflow client stub. It is used to start the workflow execution.
        GreetingWorkflow workflow = client.newWorkflowStub(GreetingWorkflow.class, workflowOptions);

        // Start workflow asynchronously and call its getGreeting workflow method
        WorkflowClient.start(workflow::getGreetings);
        // @@@SNIPEND

        sendMessages(workflow);
        System.out.println(WorkflowStub.fromTyped(workflow).getResult(String.class));
        System.exit(0);
    }

    public static void sendMessages(GreetingWorkflow workflow)
        throws InterruptedException, ExecutionException {
        // @@@SNIPSTART java-message-passing-send-query
        List<Language> languages = workflow.getLanguages(new GetLanguagesInput(false));
        System.out.println("Supported languages: " + languages);
        // @@@SNIPEND

        // @@@SNIPSTART java-message-passing-send-update
        Language previousLanguage = workflow.setLanguage(Language.CHINESE);
        // @@@SNIPEND

        // @@@SNIPSTART java-message-passing-start-update
        WorkflowUpdateHandle<Language> handle =
            WorkflowStub.fromTyped(workflow)
                .startUpdate(
                    "setLanguage", WorkflowUpdateStage.ACCEPTED, Language.class, Language.ENGLISH);
        previousLanguage = handle.getResultAsync().get();
        // @@@SNIPEND
        System.out.println("Previous language: " + previousLanguage);

        // @@@SNIPSTART java-message-passing-send-signal
        workflow.approve(new ApproveInput("Me"));
        // @@@SNIPEND
    }

    public static Language startUpdateWithStart(GreetingWorkflow workflow)
        throws InterruptedException, ExecutionException {
        // @@@SNIPSTART java-message-passing-start-update-with-start
        WorkflowUpdateHandle<Language> handle =
            WorkflowClient.startUpdateWithStart(
                workflow::setLanguage,
                Language.ENGLISH,
                UpdateOptions.<Language>newBuilder().setWaitForStage(WorkflowUpdateStage.ACCEPTED).build(),
                new WithStartWorkflowOperation<>(workflow::getGreetings));

        Language previousLanguage = handle.getResultAsync().get();
        // @@@SNIPEND
        return previousLanguage;
    }

    public static Language executeUpdateWithStart(GreetingWorkflow workflow) {
        // @@@SNIPSTART java-message-passing-execute-update-with-start
        Language previousLanguage =
            WorkflowClient.executeUpdateWithStart(
                workflow::setLanguage,
                Language.ENGLISH,
                UpdateOptions.<Language>newBuilder().build(),
                new WithStartWorkflowOperation<>(workflow::getGreetings));
        // @@@SNIPEND
        return previousLanguage;
    }
}
