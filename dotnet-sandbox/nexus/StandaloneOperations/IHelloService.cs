namespace TemporalioDocs.Nexus.StandaloneOperations;

using NexusRpc;

[NexusService]
public interface IHelloService
{
    [NexusOperation]
    EchoOutput Echo(EchoInput input);

    [NexusOperation]
    HelloOutput SayHello(HelloInput input);

    public record EchoInput(string Message);

    public record EchoOutput(string Message);

    public record HelloInput(string Name);

    public record HelloOutput(string Message);
}
