// @@@SNIPSTART dotnet-nexus-quickstart-caller-starter
using MyNamespace;
using Temporalio.Client;
using Temporalio.Worker;

var client = await TemporalClient.ConnectAsync(
    new("localhost:7233") { Namespace = "my-caller-namespace" });

using var tokenSource = new CancellationTokenSource();
Console.CancelKeyPress += (_, eventArgs) =>
{
    tokenSource.Cancel();
    eventArgs.Cancel = true;
};

using var worker = new TemporalWorker(
    client,
    new TemporalWorkerOptions(CallerWorkflow.CallerTaskQueue)
        .AddWorkflow<CallerWorkflow>());

Console.WriteLine("Running caller worker");
var workerTask = worker.ExecuteAsync(tokenSource.Token);

var result = await client.ExecuteWorkflowAsync(
    (CallerWorkflow wf) => wf.RunAsync("Temporal"),
    new(id: $"caller-workflow-{Guid.NewGuid()}",
        taskQueue: CallerWorkflow.CallerTaskQueue));
Console.WriteLine("Workflow result: {0}", result);

tokenSource.Cancel();
try { await workerTask; } catch (OperationCanceledException) { }
// @@@SNIPEND
