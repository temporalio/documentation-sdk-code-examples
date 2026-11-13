# External Storage

This sample uses Temporal .NET SDK 1.22.0 and the published
`Temporalio.Extensions.Aws.S3Driver.AwsSdk` 1.22.0 package. It includes snippets for
Amazon S3, Client and Worker configuration, a local development driver, payload
size thresholds, and driver migration.

## Requirements

- .NET 10 SDK (the example project's target framework).
- A local Temporal Service for `dotnet run`, such as `temporal server start-dev`.
- For S3: an existing bucket, AWS credentials, and a Region. Clients and Workers
  need `s3:PutObject` and `s3:GetObject` on `v0/*` and `s3:ListBucket` on the bucket.
  The project includes `AWSSDK.Signin` to support credentials from `aws login`.

## Run

From this directory, use S3:

```sh
S3_BUCKET=my-temporal-payloads AWS_REGION=us-east-1 dotnet run
```

Or leave `S3_BUCKET` unset to use local disk:

```sh
dotnet run
```

Local storage defaults to `./payload-store`; set `PAYLOAD_STORE_DIR` to change it.
Every Client and Worker must share that directory. The driver publishes complete
files without overwriting existing payloads. It is for development and does not
provide production durability.

Temporal connection options come from `ClientEnvConfig.LoadClientConnectOptions`,
with `localhost:7233` as the default address. For example, set `TEMPORAL_ADDRESS`
and `TEMPORAL_NAMESPACE` to choose a different Temporal Service.

The program sends a 3 MiB string through Workflow input, Activity input and result,
and Workflow result. It verifies the returned value and checks that all four
payloads are small External Storage references in Event History, then prints a
JSON report. A failed check exits with an error.

## Build and test

From `dotnet-sandbox`:

```sh
dotnet build temporaliodotnethelloworld.sln --configuration Release
dotnet test bestpractices/ExternalStorage.Tests --configuration Release
```

The integration tests start an isolated local Temporal Service automatically.
Optionally set `TEMPORAL_CLI_PATH` to an installed Temporal CLI executable to avoid
downloading one. The local-disk test always runs. The S3 test is skipped unless
`S3_BUCKET` is set:

```sh
S3_BUCKET=my-disposable-test-bucket AWS_REGION=us-east-1 \
  dotnet test bestpractices/ExternalStorage.Tests --configuration Release
```

Use a disposable bucket for the live test. It writes objects under `v0/` and
leaves them available for inspection. Delete the test objects and bucket after
you finish inspecting them.

Both tests exercise a real Workflow and Activity with the 3 MiB payload. They
verify the round trip and references in Event History rather than driver internals.
