namespace TemporalioDocs.Platform.Observability;

using Microsoft.Extensions.Logging;
using Temporalio.Client;

public static class ConsoleLogging
{
    public static async Task<TemporalClient> ConnectAsync()
    {
        // @@@SNIPSTART dotnet-observability-logger-factory
        var client = await TemporalClient.ConnectAsync(new("localhost:7233")
        {
            LoggerFactory = LoggerFactory.Create(builder =>
                builder.
                    AddSimpleConsole(options => options.TimestampFormat = "[HH:mm:ss] ").
                    SetMinimumLevel(LogLevel.Information)),
        });
        // @@@SNIPEND
        return client;
    }
}
