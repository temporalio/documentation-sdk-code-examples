namespace TemporalioDocs.Workers.CloudRunWorker;

// @@@SNIPSTART dotnet-cloud-run-worker-identity
using Temporalio.Client;
using Temporalio.Common.EnvConfig;
using Temporalio.Extensions.Gcp.CloudRun.Id;

public static class IdentityWorker
{
    public static async Task<TemporalClient> ConnectAsync()
    {
        var connectOptions = ClientEnvConfig.LoadClientConnectOptions();
        connectOptions.Plugins = new ITemporalClientPlugin[] { new CloudRunIdPlugin() };
        var client = await TemporalClient.ConnectAsync(connectOptions);
        return client;
    }
}
// @@@SNIPEND
