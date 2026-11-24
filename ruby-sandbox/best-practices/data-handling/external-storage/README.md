# Ruby External Storage (unreleased SDK draft)

This sample targets the pending public API in temporalio/sdk-ruby PR #688, pinned
in this directory's Gemfile. It does not run against the released temporalio
1.11.0 gem. Replace the source dependency with a published SDK version after
release. Do not present this sample as released Public Preview documentation yet.

The SDK provides the S3 driver. The separate `aws-sdk-s3` gem supplies its AWS
client. Use Ruby 3.3 or later and follow the SDK source-build requirements for the
pinned source dependency.

From this directory:

```sh
bundle install
bundle exec ruby -wc run.rb
TEMPORAL_CLI_PATH=/path/to/temporal bundle exec ruby integration_test.rb
```

To run the program, start a local Temporal Service, then run `bundle exec ruby run.rb`. It uses local disk by default; set `S3_BUCKET` and `AWS_REGION` to use S3.
Clients and Workers need GetObject and PutObject on `v0/*` and ListBucket on the
bucket. Configure AWS credentials using the AWS SDK's default credential chain.

The integration tests start a local Temporal Service and send a 3 MiB value
through Workflow input, Activity input/result, and Workflow result. They verify
the returned value and four small references in Event History. The S3 test skips
unless S3_BUCKET is set. Use a disposable bucket and remove its objects afterward.
The local driver is for development; all processes must share its directory.

## Validation of this draft

Validated with Ruby 3.4.7 against the pinned SDK Ruby source loaded ahead of the
published 1.11.0 native bridge. The local Workflow-and-Activity test passed with a
3 MiB payload, four History references, and a largest reference of 211 bytes.
This does not validate building the SDK's native extension from the pinned Git
source. Recheck against the complete published package before release.

The live S3 integration test also passed against a temporary bucket in us-east-2:
3,145,728 bytes returned intact, four History references, largest reference 528
bytes. Both local and S3 tests passed (four assertions, no skips). Confirmed two
stored objects under `v0/`, each 3,145,759 bytes, then removed all objects and the
bucket and verified cleanup.
All Ruby sample files pass `ruby -wc`.
