// @@@SNIPSTART java-nexus-quickstart-caller-workflow-interface
package helloworkflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface NexusCallerWorkflow {

    @WorkflowMethod
    String greetThroughNexus(String name);

}
// @@@SNIPEND
