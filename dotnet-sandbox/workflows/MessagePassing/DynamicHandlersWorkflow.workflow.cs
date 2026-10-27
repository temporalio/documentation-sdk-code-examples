namespace TemporalioDocs.Workflows.MessagePassing;

using Temporalio.Converters;
using Temporalio.Workflows;

[Workflow]
public class DynamicHandlersWorkflow
{
    public record MyStatusParam(string Type);

    public record DoSomethingParam(string Type);

    private readonly Dictionary<string, string> statuses = new() { ["default"] = "ready" };
    private readonly List<DoSomethingParam> pendingThings = new();

    [WorkflowRun]
    public async Task RunAsync() => await Workflow.WaitConditionAsync(() => false);

    // @@@SNIPSTART dotnet-message-passing-dynamic-query
    [WorkflowQuery(Dynamic = true)]
    public string DynamicQueryAsync(string queryName, IRawValue[] args)
    {
        var input = Workflow.PayloadConverter.ToValue<MyStatusParam>(args.Single());
        return statuses[input.Type];
    }
    // @@@SNIPEND

    // @@@SNIPSTART dotnet-message-passing-dynamic-signal
    [WorkflowSignal(Dynamic = true)]
    public async Task DynamicSignalAsync(string signalName, IRawValue[] args)
    {
        var input = Workflow.PayloadConverter.ToValue<DoSomethingParam>(args.Single());
        pendingThings.Add(input);
    }
    // @@@SNIPEND

    // @@@SNIPSTART dotnet-message-passing-dynamic-update
    [WorkflowUpdate(Dynamic = true)]
    public async Task<string> DynamicUpdateAsync(string updateName, IRawValue[] args)
    {
        var input = Workflow.PayloadConverter.ToValue<DoSomethingParam>(args.Single());
        pendingThings.Add(input);
        return statuses[input.Type];
    }
    // @@@SNIPEND
}
