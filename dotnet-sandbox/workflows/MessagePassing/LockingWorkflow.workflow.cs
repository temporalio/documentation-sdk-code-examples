namespace TemporalioDocs.Workflows.MessagePassing;

using Temporalio.Workflows;

[Workflow]
public class LockingWorkflow
{
    private int x;
    private int y;

    [WorkflowRun]
    public async Task RunAsync() => await Workflow.WaitConditionAsync(() => false);

    // @@@SNIPSTART dotnet-message-passing-bad-handler
    [WorkflowSignal]
    public async Task BadHandlerAsync()
    {
        var data = await Workflow.ExecuteActivityAsync(
            (MyActivities acts) => acts.FetchDataAsync(),
            new() { StartToCloseTimeout = TimeSpan.FromSeconds(10) });
        this.x = data.X;
        // 🐛🐛 Bug!! If multiple instances of this handler are executing concurrently, then
        // there may be times when the Workflow has this.x from one Activity execution and this.y from another.
        await Workflow.DelayAsync(1000);
        this.y = data.Y;
    }
    // @@@SNIPEND

    // @@@SNIPSTART dotnet-message-passing-safe-handler
    private readonly Mutex mutex = new();

    [WorkflowSignal]
    public async Task SafeHandlerAsync()
    {
        await mutex.WaitOneAsync();
        try
        {
            var data = await Workflow.ExecuteActivityAsync(
                (MyActivities acts) => acts.FetchDataAsync(),
                new() { StartToCloseTimeout = TimeSpan.FromSeconds(10) });
            this.x = data.X;
            // ✅ OK: the scheduler may switch now to a different handler execution, or to the main workflow
            // method, but no other execution of this handler can run until this execution finishes.
            await Workflow.DelayAsync(1000);
            this.y = data.Y;
        }
        finally
        {
            mutex.ReleaseMutex();
        }
    }
    // @@@SNIPEND
}
