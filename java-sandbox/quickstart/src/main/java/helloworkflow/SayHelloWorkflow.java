// @@@SNIPSTART java-quickstart-workflow-interface
package helloworkflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface SayHelloWorkflow {

    @WorkflowMethod
    String sayHello(String name);

}
// @@@SNIPEND
