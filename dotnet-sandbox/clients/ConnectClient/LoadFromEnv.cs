// @@@SNIPSTART dotnet-client-load-from-env
using Temporalio.Client;
using Temporalio.Common.EnvConfig;

namespace TemporalioDocs.Client.ConnectClient;

/// <summary>
/// Sample demonstrating loading the default environment configuration profile
/// from environment variables.
/// </summary>
public static class LoadFromEnv
{
    public static async Task RunAsync()
    {
        try
        {
            var connectOptions = ClientEnvConfig.LoadClientConnectOptions();

            Console.WriteLine("\nAttempting to connect to client...");

            var client = await TemporalClient.ConnectAsync(connectOptions);
            Console.WriteLine("✅ Client connected successfully!");
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            Console.WriteLine($"❌ Failed to connect: {ex.Message}");
        }
    }
}
// @@@SNIPEND
