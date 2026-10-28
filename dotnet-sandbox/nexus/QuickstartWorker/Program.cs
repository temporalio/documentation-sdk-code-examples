using MyNamespace;
using Temporalio.Client;
using Temporalio.Worker;

// The .NET quickstart Worker with the Nexus Service handler registered.
var client = await TemporalClient.ConnectAsync(new("localhost:7233"));

using var tokenSource = new CancellationTokenSource();
Console.CancelKeyPress += (_, eventArgs) =>
{
    tokenSource.Cancel();
    eventArgs.Cancel = true;
};

// @@@SNIPSTART dotnet-nexus-quickstart-worker
var activities = new MyActivities();
using var worker = new TemporalWorker(
    client,
    new TemporalWorkerOptions("my-task-queue")
        .AddActivity(activities.SayHello)
        .AddWorkflow<SayHelloWorkflow>()
        .AddNexusService(new SayHelloNexusServiceHandler()));
// @@@SNIPEND

Console.WriteLine("Running worker");
try
{
    await worker.ExecuteAsync(tokenSource.Token);
}
catch (OperationCanceledException)
{
    Console.WriteLine("Worker cancelled");
}
