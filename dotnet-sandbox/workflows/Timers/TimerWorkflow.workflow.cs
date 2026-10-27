namespace TemporalioDocs.Workflows.Timers;

using Temporalio.Workflows;

[Workflow]
public class TimerWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync()
    {
        // @@@SNIPSTART dotnet-timers-delay
        // Sleep for 3 days
        await Workflow.DelayAsync(TimeSpan.FromDays(3));
        // @@@SNIPEND
        return "Timer fired";
    }
}
