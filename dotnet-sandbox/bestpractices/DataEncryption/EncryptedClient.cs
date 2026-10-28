namespace TemporalioDocs.BestPractices.DataEncryption;

using Temporalio.Client;
using Temporalio.Converters;

public static class EncryptedClient
{
    public static async Task<TemporalClient> ConnectAsync()
    {
        // @@@SNIPSTART dotnet-data-encryption-client
        var myClient = await TemporalClient.ConnectAsync(new("localhost:7233")
        {
            DataConverter = DataConverter.Default with { PayloadCodec = new EncryptionCodec() },
        });
        // @@@SNIPEND
        return myClient;
    }
}
