// @@@SNIPSTART dotnet-nexus-quickstart-service
namespace MyNamespace;

using NexusRpc;

[NexusService]
public interface ISayHelloNexusService
{
    public static readonly string EndpointName = "my-nexus-endpoint-name";

    [NexusOperation]
    string SayHello(MyInput input);

    public record MyInput(string Name);
}
// @@@SNIPEND
