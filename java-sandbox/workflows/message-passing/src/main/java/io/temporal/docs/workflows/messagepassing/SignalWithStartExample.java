package io.temporal.docs.workflows.messagepassing;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

public class SignalWithStartExample {

    public static class Customer {
        public String name;

        public Customer() {}

        public Customer(String name) {
            this.name = name;
        }
    }

    // @@@SNIPSTART java-message-passing-signal-with-start-interface
    @WorkflowInterface
    public interface GreetingWorkflow {
        @WorkflowMethod
        String greet(Customer customer);

        @SignalMethod
        void setCustomer(Customer customer);

        @QueryMethod
        Customer getCustomer();
    }
    // @@@SNIPEND

    public static class GreetingWorkflowImpl implements GreetingWorkflow {
        private Customer customer;

        @Override
        public String greet(Customer customer) {
            // The Signal handler runs first, so a Customer it set takes precedence.
            if (this.customer == null) {
                this.customer = customer;
            }
            return "Hello " + this.customer.name;
        }

        @Override
        public void setCustomer(Customer customer) {
            this.customer = customer;
        }

        @Override
        public Customer getCustomer() {
            return customer;
        }
    }

    public static String signalWithStart(
        WorkflowClient client, String workflowId, String taskQueue, Customer customer1, Customer customer2) {
        // @@@SNIPSTART java-message-passing-signal-with-start
        // WorkflowStub is a client-side stub to a single Workflow instance
        WorkflowStub untypedWorkflowStub = client.newUntypedWorkflowStub("GreetingWorkflow",
            WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(taskQueue)
                .build());

        untypedWorkflowStub.signalWithStart("setCustomer", new Object[] {customer2}, new Object[] {customer1});

        String greeting = untypedWorkflowStub.getResult(String.class);
        // @@@SNIPEND
        return greeting;
    }
}
