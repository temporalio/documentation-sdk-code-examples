using Microsoft.Extensions.Logging;
using Temporalio.Client;
using Temporalio.Worker;
using TemporalioDocs.Nexus.FeatureGuide;
using TemporalioDocs.Nexus.FeatureGuide.Caller;
using TemporalioDocs.Nexus.FeatureGuide.Handler;

using var loggerFactory = LoggerFactory.Create(builder =>
    builder.
        AddSimpleConsole(options => options.TimestampFormat = "[HH:mm:ss] ").
        SetMinimumLevel(LogLevel.Information));
var logger = loggerFactory.CreateLogger<Program>();

// Cancellation token cancelled on ctrl+c
using var tokenSource = new CancellationTokenSource();
Console.CancelKeyPress += (_, eventArgs) =>
{
    tokenSource.Cancel();
    eventArgs.Cancel = true;
};

Task<TemporalClient> ConnectClientAsync(string currentNamespace) =>
    TemporalClient.ConnectAsync(
        new("localhost:7233")
        {
            Namespace = currentNamespace,
            LoggerFactory = loggerFactory,
        });

// @@@SNIPSTART dotnet-nexus-feature-guide-handler-worker
async Task RunHandlerWorkerAsync()
{
    // Run worker until cancelled
    logger.LogInformation("Running handler worker");
    using var worker = new TemporalWorker(
        await ConnectClientAsync("nexus-simple-handler-namespace"),
        new TemporalWorkerOptions(taskQueue: "nexus-simple-handler-sample").
            AddNexusService(new HelloService()).
            AddWorkflow<HelloHandlerWorkflow>());
    try
    {
        await worker.ExecuteAsync(tokenSource.Token);
    }
    catch (OperationCanceledException)
    {
        logger.LogInformation("Handler worker cancelled");
    }
}
// @@@SNIPEND

// @@@SNIPSTART dotnet-nexus-feature-guide-caller-worker
async Task RunCallerWorkerAsync()
{
    // Run worker until cancelled
    logger.LogInformation("Running caller worker");
    using var worker = new TemporalWorker(
        await ConnectClientAsync("nexus-simple-caller-namespace"),
        new TemporalWorkerOptions(taskQueue: "nexus-simple-caller-sample").
            AddWorkflow<EchoCallerWorkflow>().
            AddWorkflow<HelloCallerWorkflow>());
    try
    {
        await worker.ExecuteAsync(tokenSource.Token);
    }
    catch (OperationCanceledException)
    {
        logger.LogInformation("Caller worker cancelled");
    }
}
// @@@SNIPEND

// @@@SNIPSTART dotnet-nexus-feature-guide-starter
async Task ExecuteCallerWorkflowAsync()
{
    logger.LogInformation("Executing caller echo workflow");
    var client = await ConnectClientAsync("nexus-simple-caller-namespace");
    var result1 = await client.ExecuteWorkflowAsync(
        (EchoCallerWorkflow wf) => wf.RunAsync("Nexus Echo 👋"),
        new(id: "nexus-simple-echo-id", taskQueue: "nexus-simple-caller-sample"));
    logger.LogInformation("Workflow result: {Result}", result1);

    logger.LogInformation("Executing caller hello workflow");
    var result2 = await client.ExecuteWorkflowAsync(
        (HelloCallerWorkflow wf) => wf.RunAsync("Temporal", IHelloService.HelloLanguage.Es),
        new(id: "nexus-simple-hello-id", taskQueue: "nexus-simple-caller-sample"));
    logger.LogInformation("Workflow result: {Result}", result2);
}
// @@@SNIPEND

switch (args.ElementAtOrDefault(0))
{
    case "handler-worker":
        await RunHandlerWorkerAsync();
        break;
    case "caller-worker":
        await RunCallerWorkerAsync();
        break;
    case "caller-workflow":
        await ExecuteCallerWorkflowAsync();
        break;
    default:
        throw new ArgumentException("Must pass 'handler-worker', 'caller-worker', or 'caller-workflow' as the single argument");
}
