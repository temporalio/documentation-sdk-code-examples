// @@@SNIPSTART dotnet-client-manual-connect
using System;
using System.Threading.Tasks;
using Temporalio.Client;

namespace TemporalioDocs.Client.ConnectClient
{
    public static class ManualConnect
    {
        public static async Task RunAsync()
        {
            Console.WriteLine("--- Connecting manually to Temporal ---");

            var client = await TemporalClient.ConnectAsync(new TemporalClientConnectOptions
            {
                TargetHost = "localhost:7233",
                Namespace  = "default",
            });

            Console.WriteLine("✅ Connected to local Temporal service!");
        }
    }
}
// @@@SNIPEND
