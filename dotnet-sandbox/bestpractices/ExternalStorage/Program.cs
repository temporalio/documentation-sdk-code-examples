using Amazon.S3;
using System.Text.Json;
using Temporalio.Converters;
using Temporalio.Extensions.Aws.S3Driver;
using Temporalio.Extensions.Aws.S3Driver.AwsSdk;
using TemporalioDocs.ExternalStorageExample;

var bucket = Environment.GetEnvironmentVariable("S3_BUCKET");
if (!string.IsNullOrWhiteSpace(bucket))
{
    // @@@SNIPSTART dotnet-external-storage-s3-create
    using var s3Client = new AmazonS3Client();
    var driver = new S3StorageDriver(
        new AwsSdkS3StorageDriverClient(s3Client),
        bucket);
    // @@@SNIPEND
    var client = await ExampleRunner.ConnectAsync(driver);
    Console.WriteLine(JsonSerializer.Serialize(await ExampleRunner.RunAsync(client)));
}
else
{
    var directory = Environment.GetEnvironmentVariable("PAYLOAD_STORE_DIR")
        ?? Path.Combine(Environment.CurrentDirectory, "payload-store");
    IStorageDriver driver = new LocalDiskStorageDriver(directory);
    var client = await ExampleRunner.ConnectAsync(driver);
    Console.WriteLine(JsonSerializer.Serialize(await ExampleRunner.RunAsync(client)));
}
