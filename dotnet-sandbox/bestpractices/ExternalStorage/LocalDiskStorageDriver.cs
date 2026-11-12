using System.Security.Cryptography;
using Google.Protobuf;
using Temporalio.Api.Common.V1;
using Temporalio.Converters;

namespace TemporalioDocs.ExternalStorageExample;

// @@@SNIPSTART dotnet-external-storage-local-driver
// For local development only. All Clients and Workers must share this directory.
public sealed class LocalDiskStorageDriver(string directory) : IStorageDriver
{
    public string Name => "local-disk";
    public string Type => "example.local-disk";

    public async Task<IReadOnlyCollection<StorageDriverClaim>> StoreAsync(
        StorageDriverStoreContext context,
        IReadOnlyCollection<Payload> payloads,
        CancellationToken cancellationToken)
    {
        Directory.CreateDirectory(directory);
        var claims = new List<StorageDriverClaim>(payloads.Count);
        foreach (var payload in payloads)
        {
            var bytes = payload.ToByteArray();
            var key = Convert.ToHexString(SHA256.HashData(bytes)).ToLowerInvariant();
            await File.WriteAllBytesAsync(Path.Combine(directory, key), bytes, cancellationToken);
            claims.Add(new StorageDriverClaim(new Dictionary<string, string> { ["key"] = key }));
        }
        return claims;
    }

    public async Task<IReadOnlyCollection<Payload>> RetrieveAsync(
        StorageDriverRetrieveContext context,
        IReadOnlyCollection<StorageDriverClaim> claims,
        CancellationToken cancellationToken)
    {
        var payloads = new List<Payload>(claims.Count);
        foreach (var claim in claims)
        {
            if (!claim.ClaimData.TryGetValue("key", out var key)
                || key.Length != 64 || !key.All(Uri.IsHexDigit))
                throw new InvalidDataException("Invalid payload key.");
            var bytes = await File.ReadAllBytesAsync(Path.Combine(directory, key), cancellationToken);
            var checksum = Convert.ToHexString(SHA256.HashData(bytes)).ToLowerInvariant();
            if (checksum != key)
                throw new InvalidDataException("Payload checksum mismatch.");
            payloads.Add(Payload.Parser.ParseFrom(bytes));
        }
        return payloads;
    }
}
// @@@SNIPEND
