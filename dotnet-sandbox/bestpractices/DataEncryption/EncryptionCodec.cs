// @@@SNIPSTART dotnet-data-encryption-codec
namespace TemporalioDocs.BestPractices.DataEncryption;

using System.Text;
using Google.Protobuf;
using Temporalio.Api.Common.V1;
using Temporalio.Converters;

public class EncryptionCodec : IPayloadCodec
{
    public Task<IReadOnlyCollection<Payload>> EncodeAsync(IReadOnlyCollection<Payload> payloads) =>
        Task.FromResult<IReadOnlyCollection<Payload>>(payloads.Select(p =>
        {
            return new Payload()
            {
                // Set our specific encoding. We may also want to add a key ID in here for use by
                // the decode side
                Metadata = { ["encoding"] = ByteString.CopyFromUtf8("binary/my-payload-encoding") },
                Data = ByteString.CopyFrom(Encrypt(p.ToByteArray())),
            };
        }).ToList());

    public Task<IReadOnlyCollection<Payload>> DecodeAsync(IReadOnlyCollection<Payload> payloads) =>
        Task.FromResult<IReadOnlyCollection<Payload>>(payloads.Select(p =>
        {
            // Ignore if it doesn't have our expected encoding
            if (p.Metadata.GetValueOrDefault("encoding")?.ToStringUtf8() != "binary/my-payload-encoding")
            {
                return p;
            }
            // Decrypt
            return Payload.Parser.ParseFrom(Decrypt(p.Data.ToByteArray()));
        }).ToList());

    private byte[] Encrypt(byte[] data) => Encoding.ASCII.GetBytes(Convert.ToBase64String(data));

    private byte[] Decrypt(byte[] data) => Convert.FromBase64String(Encoding.ASCII.GetString(data));
}
// @@@SNIPEND
