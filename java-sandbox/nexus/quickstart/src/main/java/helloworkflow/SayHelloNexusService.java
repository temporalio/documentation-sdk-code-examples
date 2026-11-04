// @@@SNIPSTART java-nexus-quickstart-service
package helloworkflow;

import io.nexusrpc.Operation;
import io.nexusrpc.Service;

@Service
public interface SayHelloNexusService {

    @Operation
    String sayHello(String name);

}
// @@@SNIPEND
