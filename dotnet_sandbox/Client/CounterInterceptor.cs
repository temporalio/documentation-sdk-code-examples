
using Google.Protobuf;
using Temporalio.Api.Common.V1;
using Temporalio.Client;
using Temporalio.Client.Interceptors;

public static class UserContext
{
    private static readonly AsyncLocal<string?> CurrentUser = new();

    public static string? UserId
    {
        get => CurrentUser.Value;
        set => CurrentUser.Value = value;
    }
}

public class ContextPropagationInterceptor : IClientInterceptor
{
    public ClientOutboundInterceptor InterceptClient(
        ClientOutboundInterceptor nextInterceptor) =>
        new ContextPropagationClientOutboundInterceptor(nextInterceptor);
}

public class ContextPropagationClientOutboundInterceptor(
    ClientOutboundInterceptor next)
    : ClientOutboundInterceptor(next)
{
    public override Task<WorkflowHandle<TWorkflow, TResult>>
        StartWorkflowAsync<TWorkflow, TResult>(StartWorkflowInput input)
    {
        var headers = input.Headers ?? new Dictionary<string, Payload>();
        headers["user-id"] = new Payload
        {
            Metadata = { ["encoding"] = ByteString.CopyFromUtf8("plain/text") },
            Data = ByteString.CopyFromUtf8(UserContext.UserId),
        };

        return base.StartWorkflowAsync<TWorkflow, TResult>(input with { Headers = headers });
    }
}