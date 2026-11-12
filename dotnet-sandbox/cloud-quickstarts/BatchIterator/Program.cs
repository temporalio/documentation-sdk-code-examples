using Microsoft.Extensions.Logging;
using Temporalio.Client;
using Temporalio.Common.EnvConfig;
using Temporalio.Worker;
using TemporalioDocs.CloudQuickstarts.BatchIterator;

// Runs the Worker or the client: dotnet run -- worker | dotnet run -- client
switch (args.FirstOrDefault())
{
    case "worker":
        await RunWorkerAsync();
        break;
    case "client":
        await RunClientAsync();
        break;
    default:
        Console.WriteLine("Usage: dotnet run -- <worker|client>");
        break;
}

static TemporalClientConnectOptions LoadConnectOptions()
{
    // LoadClientConnectOptions reads the TEMPORAL_PROFILE env var (default
    // "default"), loads that profile from the TOML config file (default OS config
    // dir, or TEMPORAL_CONFIG_FILE), and applies any TEMPORAL_* overrides. No
    // connection details are hardcoded, so the same Worker and client run against
    // a local dev server or Temporal Cloud.
    var options = ClientEnvConfig.LoadClientConnectOptions();
    // With no config file and no env overrides the loaded options are empty, so
    // fall back to the local dev server address.
    options.TargetHost ??= "localhost:7233";
    // Show the "Processed SingleRecord { ... }" lines the child Workflows log.
    options.LoggerFactory = LoggerFactory.Create(builder =>
        builder.AddSimpleConsole(o => o.SingleLine = true).SetMinimumLevel(LogLevel.Information));
    return options;
}

static async Task RunWorkerAsync()
{
    var options = LoadConnectOptions();
    var client = await TemporalClient.ConnectAsync(options);

    using var tokenSource = new CancellationTokenSource();
    Console.CancelKeyPress += (_, eventArgs) =>
    {
        tokenSource.Cancel();
        eventArgs.Cancel = true;
    };

    using var worker = new TemporalWorker(
        client,
        new TemporalWorkerOptions(Shared.TaskQueue)
            .AddWorkflow<IteratorBatchWorkflow>()
            .AddWorkflow<RecordProcessorWorkflow>()
            .AddActivity(RecordLoader.GetRecords));

    Console.WriteLine(
        $"Worker connected to {options.TargetHost} (namespace: {options.Namespace}), " +
        $"polling task queue '{Shared.TaskQueue}'");
    try
    {
        await worker.ExecuteAsync(tokenSource.Token);
    }
    catch (OperationCanceledException)
    {
        Console.WriteLine("Worker stopped");
    }
}

static async Task RunClientAsync()
{
    var options = LoadConnectOptions();
    var client = await TemporalClient.ConnectAsync(options);

    var handle = await client.StartWorkflowAsync(
        (IteratorBatchWorkflow wf) => wf.RunAsync(Shared.PageSize, 0),
        new(id: $"batch-iterator-{DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()}", taskQueue: Shared.TaskQueue));
    Console.WriteLine(
        $"Started batch Workflow {handle.Id} against {options.TargetHost} (namespace: {options.Namespace})...");

    // GetResultAsync follows the continue-as-new chain and returns the final run's result.
    var processed = await handle.GetResultAsync();
    Console.WriteLine($"Batch complete: processed {processed} records");
}
