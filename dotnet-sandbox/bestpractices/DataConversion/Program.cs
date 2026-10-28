// @@@SNIPSTART dotnet-data-conversion-camel-case
using System.Text.Json;
using Temporalio.Client;
using Temporalio.Converters;

var client = await TemporalClient.ConnectAsync(new()
{
    TargetHost = "localhost:7233",
    Namespace = "my-namespace",
    DataConverter = DataConverter.Default with { PayloadConverter = new CamelCasePayloadConverter() },
});

public class CamelCasePayloadConverter : DefaultPayloadConverter
{
    public CamelCasePayloadConverter()
      : base(new JsonSerializerOptions { PropertyNamingPolicy = JsonNamingPolicy.CamelCase })
    {
    }
}
// @@@SNIPEND
