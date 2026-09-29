using Temporalio.Activities;
using Temporalio.Worker.Interceptors;

public class SimpleWorkerInterceptor : IWorkerInterceptor
{
    public ActivityInboundInterceptor InterceptActivity(
        ActivityInboundInterceptor nextInterceptor) =>
        new ActivityMetricsInterceptor(nextInterceptor);

    public WorkflowInboundInterceptor InterceptWorkflow(
        WorkflowInboundInterceptor nextInterceptor) =>
        nextInterceptor;

    public NexusOperationInboundInterceptor InterceptNexusOperation(
        NexusOperationInboundInterceptor nextInterceptor) =>
        nextInterceptor;
}

public class ActivityMetricsInterceptor(ActivityInboundInterceptor next)
    : ActivityInboundInterceptor(next)
{
    public override async Task<object?> ExecuteActivityAsync(
        ExecuteActivityInput input)
    {
        var info = ActivityExecutionContext.Current.Info;
        var started = DateTimeOffset.UtcNow;

        // Before the activity executes
        var scheduleToStart =
            started - info.CurrentAttemptScheduledTime;

        Console.WriteLine(
            $"Schedule-To-Start latency: {scheduleToStart}");

        // Execute the activity
        var result = await base.ExecuteActivityAsync(input);

        // After the activity completes
        var scheduleToClose =
            DateTimeOffset.UtcNow - info.CurrentAttemptScheduledTime;

        Console.WriteLine(
            $"Schedule-To-Close latency: {scheduleToClose}");

        return result;
    }
}