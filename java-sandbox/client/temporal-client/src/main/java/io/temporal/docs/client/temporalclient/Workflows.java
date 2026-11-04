package io.temporal.docs.client.temporalclient;

import io.temporal.common.converter.EncodedValues;
import io.temporal.workflow.DynamicWorkflow;
import io.temporal.workflow.ExternalWorkflowStub;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Workflow types the Client examples start. */
public class Workflows {

  @WorkflowInterface
  public interface NotifyUserAccounts {
    @WorkflowMethod
    void notify(String[] accountIds);
  }

  public static class NotifyUserAccountsImpl implements NotifyUserAccounts {
    @Override
    public void notify(String[] accountIds) {
      Workflow.getLogger(NotifyUserAccountsImpl.class).info("Notifying {}", Arrays.toString(accountIds));
    }
  }

  @WorkflowInterface
  public interface FileProcessingWorkflow {
    @WorkflowMethod
    String processFile(String fileName);
  }

  public static class FileProcessingWorkflowImpl implements FileProcessingWorkflow {
    @Override
    public String processFile(String fileName) {
      return "Processed " + fileName;
    }
  }

  @WorkflowInterface
  public interface YourWorkflowInterface {
    @WorkflowMethod
    String yourMethod();
  }

  public static class YourWorkflowImpl implements YourWorkflowInterface {
    @Override
    public String yourMethod() {
      return "done";
    }
  }

  public static class DynamicGreetingWorkflowImpl implements DynamicWorkflow {
    @Override
    public Object execute(EncodedValues args) {
      // @@@SNIPSTART java-temporal-client-dynamic-workflow-type
      String type = Workflow.getInfo().getWorkflowType();
      // @@@SNIPEND
      return type + ": Hello " + args.get(0, String.class) + "!";
    }
  }

  @WorkflowInterface
  public interface OrderWorkflow {
    @WorkflowMethod
    List<String> run();

    @SignalMethod
    void addItem(String item);
  }

  public static class OrderWorkflowImpl implements OrderWorkflow {
    private final List<String> items = new ArrayList<>();

    @Override
    public List<String> run() {
      Workflow.await(() -> !items.isEmpty());
      return items;
    }

    @Override
    public void addItem(String item) {
      items.add(item);
    }
  }

  @WorkflowInterface
  public interface YourWorkflow {
    @WorkflowMethod
    String yourWFMethod(String name);
  }

  public static class YourWorkflowSignalingImpl implements YourWorkflow {
    // @@@SNIPSTART java-temporal-client-external-workflow-stub
    @Override
    public String yourWFMethod(String name) {
      // Get a stub for a running Workflow Execution by its Workflow ID
      ExternalWorkflowStub otherWorkflow = Workflow.newUntypedExternalWorkflowStub("OtherWFId");
      // Send a Signal by name; the target Workflow can be written in any SDK
      otherWorkflow.signal("addItem", name);
      return "Signaled OtherWFId";
    }
    // @@@SNIPEND
  }

  @WorkflowInterface
  public interface CronWorkflow {
    @WorkflowMethod
    String cronWorkflow();
  }

  public static class CronWorkflowImpl implements CronWorkflow {
    // @@@SNIPSTART java-temporal-client-last-completion-result
    @Override
    public String cronWorkflow() {
      String lastProcessedFileName = Workflow.getLastCompletionResult(String.class);

      // Process work starting from the lastProcessedFileName.
      // Business logic implementation goes here.
      // Updates lastProcessedFileName to the new value.

      return lastProcessedFileName;
    }
    // @@@SNIPEND
  }
}
