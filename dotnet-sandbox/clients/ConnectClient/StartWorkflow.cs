namespace TemporalioDocs.Client.ConnectClient;

using Temporalio.Client;

public static class StartWorkflow
{
    public static async Task ExecuteAsync(ITemporalClient client)
    {
        // @@@SNIPSTART dotnet-client-execute-workflow
        var result = await client.ExecuteWorkflowAsync(
            (MyWorkflow wf) => wf.RunAsync(),
            new(id: "my-workflow-id", taskQueue: "my-task-queue"));
        Console.WriteLine("Result: {0}", result);
        // @@@SNIPEND
    }

    public static async Task<string> ExecuteWithOptionsAsync(ITemporalClient client)
    {
        // @@@SNIPSTART dotnet-client-workflow-options
        var result = await client.ExecuteWorkflowAsync(
            (MyWorkflow wf) => wf.RunAsync(),
            new(id: "my-workflow-id", taskQueue: "my-task-queue"));
        // @@@SNIPEND
        return result;
    }

    public static async Task GetResultAsync(ITemporalClient client)
    {
        // @@@SNIPSTART dotnet-client-get-result
        var handle = client.GetWorkflowHandle("my-workflow-id");
        var result = await handle.GetResultAsync<string>();
        Console.WriteLine("Result: {0}", result);
        // @@@SNIPEND
    }
}
