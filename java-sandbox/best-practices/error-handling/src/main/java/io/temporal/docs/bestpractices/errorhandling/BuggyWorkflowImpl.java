package io.temporal.docs.bestpractices.errorhandling;

// Throws a plain exception, which fails the Workflow Task rather than the Workflow Execution
// unless the type is listed in WorkflowImplementationOptions.setFailWorkflowExceptionTypes().
public class BuggyWorkflowImpl implements OrderWorkflow {
    @Override
    public Result processOrder(Order order) {
        throw new IllegalStateException("bug in Workflow code");
    }
}
