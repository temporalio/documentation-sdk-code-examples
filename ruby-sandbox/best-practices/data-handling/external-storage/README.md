# Ruby External Storage

The SDK provides the S3 driver. The separate `aws-sdk-s3` gem supplies its AWS
client. Use Ruby 3.3 or later and a released `temporalio` gem with External Storage
support.

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

CI installs the published gems and runs the local-disk integration test. The S3
test requires AWS credentials and a bucket and is run separately.
