using Amazon.S3;
using Temporalio.Converters;
using Temporalio.Extensions.Aws.S3Driver;
using Temporalio.Extensions.Aws.S3Driver.AwsSdk;
using Temporalio.Testing;
using TemporalioDocs.ExternalStorageExample;

namespace TemporalioDocs.ExternalStorageExample.Tests;

public class ExternalStorageTests
{
    [Fact]
    public async Task LocalDisk_WorkflowAndActivityRoundTrip()
    {
        var directory = Path.Combine(Path.GetTempPath(), $"temporal-dotnet-storage-{Guid.NewGuid():N}");
        try
        {
            var driver = new LocalDiskStorageDriver(directory);
            await Task.WhenAll(VerifyRoundTripAsync(driver), VerifyRoundTripAsync(driver));
        }
        finally
        {
            if (Directory.Exists(directory))
                Directory.Delete(directory, recursive: true);
        }
    }

    [S3Fact]
    public async Task S3_WorkflowAndActivityRoundTrip()
    {
        using var s3Client = new AmazonS3Client();
        var driver = new S3StorageDriver(new AwsSdkS3StorageDriverClient(s3Client),
            Environment.GetEnvironmentVariable("S3_BUCKET")!);
        await VerifyRoundTripAsync(driver);
    }

    private static async Task VerifyRoundTripAsync(IStorageDriver driver)
    {
        await using var env = await WorkflowEnvironment.StartLocalAsync(new()
        {
            DataConverter = DataConverter.Default with { ExternalStorage = new ExternalStorage(driver) },
            DevServerOptions = new()
            {
                ExistingPath = Environment.GetEnvironmentVariable("TEMPORAL_CLI_PATH"),
                StartupTimeout = TimeSpan.FromSeconds(20),
            },
        });
        var result = await ExampleRunner.RunAsync(env.Client);
        Assert.Equal(3 * 1024 * 1024, result.ResultBytes);
        Assert.Equal(4, result.OffloadedPayloadCount);
        Assert.InRange(result.LargestReferenceBytes, 1, 1023);
    }
}

public sealed class S3FactAttribute : FactAttribute
{
    public S3FactAttribute()
    {
        if (string.IsNullOrWhiteSpace(Environment.GetEnvironmentVariable("S3_BUCKET")))
            Skip = "Set S3_BUCKET and configure AWS credentials to run the live S3 integration test.";
    }
}
