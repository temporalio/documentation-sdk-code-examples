module my-org/quickstart

go 1.26.0

require go.temporal.io/sdk v1.49.0

require github.com/uber-go/tally/v4 v4.1.1

require (
	cloud.google.com/go/storage v1.69.0
	github.com/aws/aws-sdk-go-v2 v1.47.3
	github.com/aws/aws-sdk-go-v2/config v1.33.9
	github.com/aws/aws-sdk-go-v2/service/s3 v1.114.3
	go.temporal.io/sdk/contrib/aws/s3driver v0.3.0
	go.temporal.io/sdk/contrib/aws/s3driver/awssdkv2 v0.3.0
	go.temporal.io/sdk/contrib/gcp/gcsdriver v0.2.0
	go.temporal.io/sdk/contrib/gcp/gcsdriver/gcssdk v0.2.0
	go.temporal.io/sdk/contrib/workflowstreams v0.1.1
)

require (
	cel.dev/expr v0.25.2 // indirect
	cloud.google.com/go v0.123.0 // indirect
	cloud.google.com/go/auth v0.20.0 // indirect
	cloud.google.com/go/auth/oauth2adapt v0.2.8 // indirect
	cloud.google.com/go/compute/metadata v0.9.0 // indirect
	cloud.google.com/go/iam v1.12.0 // indirect
	cloud.google.com/go/monitoring v1.30.0 // indirect
	github.com/GoogleCloudPlatform/opentelemetry-operations-go/detectors/gcp v1.35.0 // indirect
	github.com/GoogleCloudPlatform/opentelemetry-operations-go/exporter/metric v0.57.0 // indirect
	github.com/GoogleCloudPlatform/opentelemetry-operations-go/internal/resourcemapping v0.57.0 // indirect
	github.com/aws/aws-sdk-go-v2/aws/protocol/eventstream v1.7.22 // indirect
	github.com/aws/aws-sdk-go-v2/credentials v1.20.9 // indirect
	github.com/aws/aws-sdk-go-v2/feature/ec2/imds v1.20.3 // indirect
	github.com/aws/aws-sdk-go-v2/internal/configsources v1.5.6 // indirect
	github.com/aws/aws-sdk-go-v2/internal/endpoints/v2 v2.8.6 // indirect
	github.com/aws/aws-sdk-go-v2/internal/v4a v1.5.6 // indirect
	github.com/aws/aws-sdk-go-v2/service/internal/accept-encoding v1.13.21 // indirect
	github.com/aws/aws-sdk-go-v2/service/internal/checksum v1.11.7 // indirect
	github.com/aws/aws-sdk-go-v2/service/internal/presigned-url v1.14.6 // indirect
	github.com/aws/aws-sdk-go-v2/service/internal/s3shared v1.20.6 // indirect
	github.com/aws/aws-sdk-go-v2/service/signin v1.10.4 // indirect
	github.com/aws/aws-sdk-go-v2/service/sso v1.38.4 // indirect
	github.com/aws/aws-sdk-go-v2/service/ssooidc v1.43.4 // indirect
	github.com/aws/aws-sdk-go-v2/service/sts v1.51.4 // indirect
	github.com/aws/smithy-go v1.28.5 // indirect
	github.com/cncf/xds/go v0.0.0-20260202195803-dba9d589def2 // indirect
	github.com/envoyproxy/go-control-plane/envoy v1.37.0 // indirect
	github.com/envoyproxy/protoc-gen-validate v1.3.3 // indirect
	github.com/felixge/httpsnoop v1.0.4 // indirect
	github.com/go-jose/go-jose/v4 v4.1.4 // indirect
	github.com/google/s2a-go v0.1.9 // indirect
	github.com/googleapis/enterprise-certificate-proxy v0.3.17 // indirect
	github.com/googleapis/gax-go/v2 v2.26.2 // indirect
	github.com/spiffe/go-spiffe/v2 v2.7.0 // indirect
	go.opentelemetry.io/contrib/detectors/gcp v1.45.0 // indirect
	go.opentelemetry.io/contrib/instrumentation/google.golang.org/grpc/otelgrpc v0.68.0 // indirect
	go.opentelemetry.io/contrib/instrumentation/net/http/otelhttp v0.67.0 // indirect
	golang.org/x/crypto v0.55.0 // indirect
	golang.org/x/oauth2 v0.36.0 // indirect
	google.golang.org/api v0.288.0 // indirect
	google.golang.org/genproto v0.0.0-20260715232425-e75dac1f907d // indirect
)

require (
	github.com/BurntSushi/toml v1.4.0 // indirect
	github.com/DataDog/datadog-agent/comp/core/tagger/origindetection v0.77.0 // indirect
	github.com/DataDog/datadog-agent/pkg/obfuscate v0.77.0 // indirect
	github.com/DataDog/datadog-agent/pkg/opentelemetry-mapping-go/otlp/attributes v0.77.0 // indirect
	github.com/DataDog/datadog-agent/pkg/proto v0.77.0 // indirect
	github.com/DataDog/datadog-agent/pkg/remoteconfig/state v0.77.0 // indirect
	github.com/DataDog/datadog-agent/pkg/template v0.77.0 // indirect
	github.com/DataDog/datadog-agent/pkg/trace v0.77.0 // indirect
	github.com/DataDog/datadog-agent/pkg/trace/log v0.77.0 // indirect
	github.com/DataDog/datadog-agent/pkg/trace/otel v0.77.0 // indirect
	github.com/DataDog/datadog-agent/pkg/trace/stats v0.77.0 // indirect
	github.com/DataDog/datadog-agent/pkg/trace/traceutil v0.77.0 // indirect
	github.com/DataDog/datadog-agent/pkg/util/log v0.77.0 // indirect
	github.com/DataDog/datadog-agent/pkg/util/scrubber v0.77.0 // indirect
	github.com/DataDog/datadog-agent/pkg/version v0.77.0 // indirect
	github.com/DataDog/datadog-go/v5 v5.8.3 // indirect
	github.com/DataDog/dd-trace-go/v2 v2.8.1 // indirect
	github.com/DataDog/go-libddwaf/v4 v4.9.0 // indirect
	github.com/DataDog/go-runtime-metrics-internal v0.0.4-0.20260217080614-b0f4edc38a6d // indirect
	github.com/DataDog/go-sqllexer v0.1.13 // indirect
	github.com/DataDog/go-tuf v1.1.1-0.5.2 // indirect
	github.com/DataDog/sketches-go v1.4.8 // indirect
	github.com/Microsoft/go-winio v0.6.2 // indirect
	github.com/aws/aws-lambda-go v1.47.0 // indirect
	github.com/beorn7/perks v1.0.1 // indirect
	github.com/cenkalti/backoff/v5 v5.0.3 // indirect
	github.com/cihub/seelog v0.0.0-20170130134532-f561c5e57575 // indirect
	github.com/dustin/go-humanize v1.0.1 // indirect
	github.com/ebitengine/purego v0.10.0 // indirect
	github.com/go-ole/go-ole v1.3.0 // indirect
	github.com/golang/snappy v0.0.4
	github.com/hashicorp/go-version v1.8.0 // indirect
	github.com/json-iterator/go v1.1.12 // indirect
	github.com/klauspost/compress v1.18.4 // indirect
	github.com/klauspost/cpuid/v2 v2.3.0 // indirect
	github.com/linkdata/deadlock v0.5.5 // indirect
	github.com/lufia/plan9stats v0.0.0-20260216142805-b3301c5f2a88 // indirect
	github.com/minio/simdjson-go v0.4.5 // indirect
	github.com/modern-go/concurrent v0.0.0-20180306012644-bacd9c7ef1dd // indirect
	github.com/modern-go/reflect2 v1.0.3-0.20250322232337-35a7c28c31ee // indirect
	github.com/munnerz/goautoneg v0.0.0-20191010083416-a7dc8b61c822 // indirect
	github.com/opentracing/opentracing-go v1.2.0 // indirect
	github.com/outcaste-io/ristretto v0.2.3 // indirect
	github.com/petermattis/goid v0.0.0-20260226131333-17d1149c6ac6 // indirect
	github.com/philhofer/fwd v1.2.0 // indirect
	github.com/pkg/errors v0.9.1 // indirect
	github.com/planetscale/vtprotobuf v0.6.1-0.20240319094008-0393e58bdf10 // indirect
	github.com/power-devops/perfstat v0.0.0-20240221224432-82ca36839d55 // indirect
	github.com/prometheus/client_golang v1.23.2 // indirect
	github.com/prometheus/client_model v0.6.2 // indirect
	github.com/prometheus/common v0.67.5 // indirect
	github.com/prometheus/procfs v0.19.2 // indirect
	github.com/puzpuzpuz/xsync/v3 v3.5.1 // indirect
	github.com/secure-systems-lab/go-securesystemslib v0.10.0 // indirect
	github.com/shirou/gopsutil/v4 v4.26.2 // indirect
	github.com/tinylib/msgp v1.6.3 // indirect
	github.com/tklauser/go-sysconf v0.3.16 // indirect
	github.com/tklauser/numcpus v0.11.0 // indirect
	github.com/trailofbits/go-mutexasserts v0.0.0-20250514102930-c1f3d2e37561 // indirect
	github.com/yusufpapurcu/wmi v1.2.4 // indirect
	go.opentelemetry.io/collector/component v1.51.1-0.20260205185216-81bc641f26c0 // indirect
	go.opentelemetry.io/collector/featuregate v1.51.1-0.20260205185216-81bc641f26c0 // indirect
	go.opentelemetry.io/collector/pdata v1.51.1-0.20260205185216-81bc641f26c0 // indirect
	go.opentelemetry.io/collector/pdata/pprofile v0.145.1-0.20260205185216-81bc641f26c0 // indirect
	go.opentelemetry.io/contrib/propagators/aws v1.42.0 // indirect
	go.opentelemetry.io/otel/exporters/otlp/otlpmetric/otlpmetricgrpc v1.42.0 // indirect
	go.opentelemetry.io/otel/exporters/otlp/otlptrace v1.45.0 // indirect
	go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracegrpc v1.42.0 // indirect
	go.opentelemetry.io/otel/sdk v1.45.0 // indirect
	go.opentelemetry.io/otel/sdk/metric v1.45.0 // indirect
	go.opentelemetry.io/proto/otlp v1.11.0 // indirect
	go.temporal.io/sdk/contrib/aws/lambdaworker v0.1.1
	go.temporal.io/sdk/contrib/aws/lambdaworker/otel v0.1.1
	go.temporal.io/sdk/contrib/datadog v0.6.0
	go.temporal.io/sdk/contrib/envconfig v1.0.0
	go.temporal.io/sdk/contrib/gcp/cloudrun/id v0.1.0
	go.temporal.io/sdk/contrib/gcp/cloudrun/otel v0.1.0
	go.temporal.io/sdk/contrib/opentelemetry/otlpworker v0.1.0 // indirect
	go.temporal.io/sdk/contrib/opentracing v0.3.0
	go.uber.org/multierr v1.11.0 // indirect
	go.uber.org/zap v1.28.0
	go.uber.org/zap/exp v0.3.0
	go.yaml.in/yaml/v2 v2.4.3 // indirect
	go.yaml.in/yaml/v3 v3.0.4 // indirect
	golang.org/x/exp v0.0.0-20260209203927-2842357ff358 // indirect
	golang.org/x/mod v0.38.0 // indirect
	golang.org/x/xerrors v0.0.0-20240903120638-7835f813f4da // indirect
	gopkg.in/ini.v1 v1.67.1 // indirect
)

require (
	github.com/cespare/xxhash/v2 v2.3.0 // indirect
	github.com/davecgh/go-spew v1.1.2-0.20180830191138-d8f796af33cc // indirect
	github.com/facebookgo/clock v0.0.0-20150410010913-600d898af40a // indirect
	github.com/go-logr/logr v1.4.4 // indirect
	github.com/go-logr/stdr v1.2.2 // indirect
	github.com/gogo/protobuf v1.3.2 // indirect
	github.com/golang/mock v1.7.0-rc.1 // indirect
	github.com/google/uuid v1.6.0
	github.com/grpc-ecosystem/go-grpc-middleware/v2 v2.3.2 // indirect
	github.com/grpc-ecosystem/grpc-gateway/v2 v2.29.0 // indirect
	github.com/nexus-rpc/nexus-proto-annotations v0.1.0 // indirect
	github.com/nexus-rpc/sdk-go v0.7.0
	github.com/pmezard/go-difflib v1.0.1-0.20181226105442-5d4384ee4fb2 // indirect
	github.com/robfig/cron v1.2.0 // indirect
	github.com/stretchr/objx v0.5.2 // indirect
	github.com/stretchr/testify v1.11.1
	github.com/twmb/murmur3 v1.2.0 // indirect
	go.opentelemetry.io/auto/sdk v1.2.1 // indirect
	go.opentelemetry.io/otel v1.45.0
	go.opentelemetry.io/otel/metric v1.45.0 // indirect
	go.opentelemetry.io/otel/trace v1.45.0 // indirect
	go.temporal.io/api v1.63.5
	go.temporal.io/sdk/contrib/opentelemetry v0.8.1
	go.temporal.io/sdk/contrib/tally v0.2.0
	go.uber.org/atomic v1.11.0 // indirect
	golang.org/x/net v0.58.0 // indirect
	golang.org/x/sync v0.22.0 // indirect
	golang.org/x/sys v0.47.0 // indirect
	golang.org/x/text v0.41.0 // indirect
	golang.org/x/time v0.15.0 // indirect
	google.golang.org/genproto/googleapis/api v0.0.0-20260720211330-0afa2a65878a // indirect
	google.golang.org/genproto/googleapis/rpc v0.0.0-20260720211330-0afa2a65878a // indirect
	google.golang.org/grpc v1.83.2
	google.golang.org/protobuf v1.36.11
	gopkg.in/yaml.v3 v3.0.1 // indirect
)
