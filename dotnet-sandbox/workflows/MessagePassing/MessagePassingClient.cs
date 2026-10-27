namespace TemporalioDocs.Workflows.MessagePassing;

using Temporalio.Client;

public static class MessagePassingClient
{
    public static async Task SendMessagesAsync()
    {
        // @@@SNIPSTART dotnet-message-passing-start-workflow
        var client = await TemporalClient.ConnectAsync(new("localhost:7233"));
        var workflowHandle = await client.StartWorkflowAsync(
            (GreetingWorkflow wf) => wf.RunAsync(),
            new(id: "message-passing-workflow-id", taskQueue: "message-passing-sample"));
        // @@@SNIPEND

        // @@@SNIPSTART dotnet-message-passing-send-query
        var supportedLanguages = await workflowHandle.QueryAsync(wf => wf.GetLanguages(new(false)));
        // @@@SNIPEND

        // @@@SNIPSTART dotnet-message-passing-send-signal
        await workflowHandle.SignalAsync(wf => wf.ApproveAsync(new("MyUser")));
        // @@@SNIPEND

        // @@@SNIPSTART dotnet-message-passing-execute-update
        var previousLanguage = await workflowHandle.ExecuteUpdateAsync(
            wf => wf.SetCurrentLanguageAsync(GreetingWorkflow.Language.Chinese));
        // @@@SNIPEND

        // @@@SNIPSTART dotnet-message-passing-start-update
        // Wait until the update is accepted
        var updateHandle = await workflowHandle.StartUpdateAsync(
            wf => wf.SetLanguageAsync(GreetingWorkflow.Language.Arabic),
            new(waitForStage: WorkflowUpdateStage.Accepted));
        // Wait until the update is completed
        var updateResult = await updateHandle.GetResultAsync();
        // @@@SNIPEND
    }

    public static async Task SignalWithStartAsync()
    {
        // @@@SNIPSTART dotnet-message-passing-signal-with-start
        var client = await TemporalClient.ConnectAsync(new("localhost:7233"));
        var options = new WorkflowOptions(id: "your-signal-with-start-workflow", taskQueue: "signal-tq");
        options.SignalWithStart((GreetingWorkflow wf) => wf.SubmitGreetingAsync("User Signal with Start"));
        await client.StartWorkflowAsync((GreetingWorkflow wf) => wf.RunAsync(), options);
        // @@@SNIPEND
    }
}
