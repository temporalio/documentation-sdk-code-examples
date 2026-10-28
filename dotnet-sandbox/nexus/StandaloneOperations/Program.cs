using Microsoft.Extensions.Logging;
using Temporalio.Client;
using Temporalio.Common.EnvConfig;
using Temporalio.Worker;
using TemporalioDocs.Nexus.StandaloneOperations;

using var loggerFactory = LoggerFactory.Create(builder =>
    builder.
        AddSimpleConsole(options => options.TimestampFormat = "[HH:mm:ss] ").
        SetMinimumLevel(LogLevel.Information));
var logger = loggerFactory.CreateLogger<Program>();

// Reads TEMPORAL_ADDRESS, TEMPORAL_NAMESPACE, and other settings from the environment.
var connectOptions = ClientEnvConfig.LoadClientConnectOptions();
connectOptions.TargetHost ??= "localhost:7233";
connectOptions.LoggerFactory = loggerFactory;
var client = await TemporalClient.ConnectAsync(connectOptions);

switch (args.ElementAtOrDefault(0))
{
    case "worker":
        using (var tokenSource = new CancellationTokenSource())
        {
            Console.CancelKeyPress += (_, eventArgs) =>
            {
                tokenSource.Cancel();
                eventArgs.Cancel = true;
            };
            using var worker = new TemporalWorker(
                client,
                new TemporalWorkerOptions("nexus-handler-queue").
                    AddNexusService(new HelloService()).
                    AddWorkflow<HelloHandlerWorkflow>());
            logger.LogInformation("Running handler worker");
            try
            {
                await worker.ExecuteAsync(tokenSource.Token);
            }
            catch (OperationCanceledException)
            {
                logger.LogInformation("Handler worker cancelled");
            }
        }
        break;
    case "starter":
        await Starter.ExecuteOperationAsync(client, logger);
        await Starter.GetHandleAsync(client, logger);
        await Starter.ListOperationsAsync(client, logger);
        await Starter.CountOperationsAsync(client, logger);
        break;
    case "starter-untyped":
        await Starter.StartOperationAsync(client, logger);
        break;
    default:
        throw new ArgumentException("Must pass 'worker', 'starter', or 'starter-untyped' as the single argument");
}
