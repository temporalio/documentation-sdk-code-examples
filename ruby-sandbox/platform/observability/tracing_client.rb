# frozen_string_literal: true

require 'opentelemetry/sdk'

my_otel_tracer_provider = OpenTelemetry::SDK::Trace::TracerProvider.new

# @@@SNIPSTART ruby-observability-tracing
require 'opentelemetry-api'
require 'opentelemetry/sdk'
require 'temporalio/client'
require 'temporalio/contrib/open_telemetry'

# ... assumes my_otel_tracer_provider is a tracer provider created by the user
my_tracer = my_otel_tracer_provider.tracer('my-otel-tracer')

my_client = Temporalio::Client.connect(
  'localhost:7233', 'my-namespace',
  interceptors: [Temporalio::Contrib::OpenTelemetry::TracingInterceptor.new(my_tracer)]
)
# @@@SNIPEND

puts "Connected to #{my_client.namespace} with tracing"
