// @@@SNIPSTART java-nexus-quickstart-handler
package helloworkflow;

import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.client.WorkflowOptions;
import io.temporal.nexus.Nexus;
import io.temporal.nexus.WorkflowRunOperation;

@ServiceImpl(service = SayHelloNexusService.class)
public class SayHelloNexusServiceImpl {

    @OperationImpl
    public OperationHandler<String, String> sayHello() {
        return WorkflowRunOperation.fromWorkflowMethod(
            (ctx, details, name) ->
                Nexus.getOperationContext()
                    .getWorkflowClient()
                    .newWorkflowStub(
                        SayHelloWorkflow.class,
                        WorkflowOptions.newBuilder()
                            .setWorkflowId("say-hello-nexus-" + details.getRequestId())
                            .build())
                    ::sayHello
        );
    }

}
// @@@SNIPEND
