namespace TemporalioDocs.Client.ConnectClient;

using Temporalio.Client;
using Temporalio.Worker;

public static class CloudConnect
{
    public static async Task<TemporalClient> ConnectWithApiKeyAsync(string endpoint)
    {
        // @@@SNIPSTART dotnet-client-cloud-api-key
        var myClient = await TemporalClient.ConnectAsync(new(endpoint)
        {
            Namespace = "<namespace_id>.<account_id>",
            ApiKey = "<APIKey>",
            Tls = new(),
        });
        // @@@SNIPEND
        return myClient;
    }

    public static void UpdateApiKey(TemporalClient myClient, string myKeyUpdated)
    {
        // @@@SNIPSTART dotnet-client-cloud-update-api-key
        myClient.Connection.ApiKey = myKeyUpdated;
        // @@@SNIPEND
    }

    public static async Task<TemporalClient> ConnectWithMtlsAsync(string endpoint)
    {
        // @@@SNIPSTART dotnet-client-cloud-mtls
        var myClient = await TemporalClient.ConnectAsync(new(endpoint)
        {
            Namespace = "<namespace_id>.<account_id>",
            Tls = new()
            {
                ClientCert = File.ReadAllBytes("client-cert.pem"),
                ClientPrivateKey = File.ReadAllBytes("client-private-key.pem"),
            },
        });
        // @@@SNIPEND
        return myClient;
    }

    public static async Task RotateCertificateAsync(string endpoint, TemporalWorker worker)
    {
        // @@@SNIPSTART dotnet-client-cloud-rotate-certificate
        var newClient = await TemporalClient.ConnectAsync(new(endpoint)
        {
            Namespace = "<namespace_id>.<account_id>",
            Tls = new()
            {
                ClientCert = File.ReadAllBytes("client-cert-new.pem"),
                ClientPrivateKey = File.ReadAllBytes("client-private-key-new.pem"),
            },
        });

        // worker is the TemporalWorker instance already running against the old client
        worker.Client = newClient;
        // @@@SNIPEND
    }
}
