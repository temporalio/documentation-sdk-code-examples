namespace TemporalioDocs.Workflows.WorkflowBasics;

using Microsoft.Extensions.Logging;
using Temporalio.Workflows;

[Workflow]
public class DeterministicApisWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync(string name)
    {
        // @@@SNIPSTART dotnet-workflow-basics-logger
        Workflow.Logger.LogInformation("Starting workflow for {Name}", name);
        // @@@SNIPEND

        // @@@SNIPSTART dotnet-workflow-basics-random
        var value = Workflow.Random.Next(1, 100);
        var uniqueId = Workflow.NewGuid();
        // @@@SNIPEND

        // @@@SNIPSTART dotnet-workflow-basics-now
        var currentTime = Workflow.UtcNow;
        // @@@SNIPEND

        // @@@SNIPSTART dotnet-workflow-basics-is-replaying
        if (!Workflow.Unsafe.IsReplaying)
        {
            EmitMetric("workflow_started", 1);
        }
        // @@@SNIPEND

        return $"{name} {value} {uniqueId} {currentTime:O}";
    }

    private static void EmitMetric(string name, long value) =>
        Workflow.MetricMeter.CreateCounter<long>(name).Add(value);
}
