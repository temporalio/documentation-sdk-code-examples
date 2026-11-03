package io.temporal.docs.workflows.versioning;

import io.temporal.worker.Worker;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

public class PizzaCutoverExample {

    public static class PizzaOrder {
        public String orderNumber;

        public PizzaOrder() {}

        public PizzaOrder(String orderNumber) {
            this.orderNumber = orderNumber;
        }
    }

    public static class OrderConfirmation {
        public String orderNumber;
        public String status;

        public OrderConfirmation() {}

        public OrderConfirmation(String orderNumber, String status) {
            this.orderNumber = orderNumber;
            this.status = status;
        }
    }

    // @@@SNIPSTART java-versioning-cutover
    @WorkflowInterface
    public interface PizzaWorkflow {

        @WorkflowMethod
        OrderConfirmation pizzaWorkflow(PizzaOrder order);
    }

    public static class PizzaWorkflowImpl implements PizzaWorkflow {

        @Override
        public OrderConfirmation pizzaWorkflow(PizzaOrder order) {
            // implementation code omitted for this example
            return new OrderConfirmation(order.orderNumber, "SUCCESS");
        }
    }

    @WorkflowInterface
    public interface PizzaWorkflowV2 {

        @WorkflowMethod
        OrderConfirmation pizzaWorkflow(PizzaOrder order);
    }

    public static class PizzaWorkflowImplV2 implements PizzaWorkflowV2 {

        @Override
        public OrderConfirmation pizzaWorkflow(PizzaOrder order) {
            // implementation code omitted for this example
            return new OrderConfirmation(order.orderNumber, "SUCCESS");
        }
    }
    // @@@SNIPEND

    public static void registerBoth(Worker worker) {
        // @@@SNIPSTART java-versioning-cutover-register
        worker.registerWorkflowImplementationTypes(PizzaWorkflowImpl.class);
        worker.registerWorkflowImplementationTypes(PizzaWorkflowImplV2.class);
        // @@@SNIPEND
    }
}
