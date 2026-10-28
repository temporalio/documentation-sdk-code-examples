namespace TemporalioDocs.Nexus.StandaloneOperations;

using Microsoft.Extensions.Logging;
using Temporalio.Client;

public static class Starter
{
    public static async Task ExecuteOperationAsync(ITemporalClient client, ILogger logger)
    {
        // @@@SNIPSTART dotnet-nexus-standalone-execute
        var nexusClient = client.CreateNexusClient<IHelloService>("my-nexus-endpoint");

        var result = await nexusClient.ExecuteNexusOperationAsync(
            svc => svc.Echo(new("Nexus Echo 👋")),
            new("unique-operation-id")
            {
                ScheduleToCloseTimeout = TimeSpan.FromSeconds(10),
            });
        // @@@SNIPEND
        logger.LogInformation("Execute result: {Message}", result.Message);
    }

    public static async Task StartOperationAsync(ITemporalClient client, ILogger logger)
    {
        // @@@SNIPSTART dotnet-nexus-standalone-start-untyped
        var nexusClient = client.CreateNexusClient("my-nexus-endpoint", "HelloService");

        var handle = await nexusClient.StartNexusOperationAsync<IHelloService.EchoOutput>(
            "Echo",
            new IHelloService.EchoInput("Nexus Echo 👋"),
            new("unique-operation-id")
            {
                ScheduleToCloseTimeout = TimeSpan.FromSeconds(10),
            });
        // @@@SNIPEND

        // @@@SNIPSTART dotnet-nexus-standalone-get-result
        var output = await handle.GetResultAsync();
        logger.LogInformation("Operation result: {Message}", output.Message);
        // @@@SNIPEND
    }

    public static async Task GetHandleAsync(ITemporalClient client, ILogger logger)
    {
        // @@@SNIPSTART dotnet-nexus-standalone-get-handle
        var handle = client.GetNexusOperationHandle<IHelloService.EchoOutput>("unique-operation-id");
        var output = await handle.GetResultAsync();
        // @@@SNIPEND
        logger.LogInformation("Recovered handle result: {Message}", output.Message);
    }

    public static async Task ListOperationsAsync(ITemporalClient client, ILogger logger)
    {
        // @@@SNIPSTART dotnet-nexus-standalone-list
        await foreach (var execution in client.ListNexusOperationsAsync(
            "Endpoint = 'my-nexus-endpoint'"))
        {
            logger.LogInformation(
                "OperationID: {Id}, Operation: {Operation}, Status: {Status}",
                execution.OperationId, execution.Operation, execution.Status);
        }
        // @@@SNIPEND
    }

    public static async Task CountOperationsAsync(ITemporalClient client, ILogger logger)
    {
        // @@@SNIPSTART dotnet-nexus-standalone-count
        var count = await client.CountNexusOperationsAsync(
            "Endpoint = 'my-nexus-endpoint'");
        logger.LogInformation("Total Nexus operations: {Count}", count.Count);
        // @@@SNIPEND
    }
}
