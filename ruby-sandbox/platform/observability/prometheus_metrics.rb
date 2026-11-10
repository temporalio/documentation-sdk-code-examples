# frozen_string_literal: true

require 'temporalio/runtime'

# Call before creating any client.
def configure_prometheus_metrics
  # @@@SNIPSTART ruby-observability-prometheus
  Temporalio::Runtime.default = Temporalio::Runtime.new(
    telemetry: Temporalio::Runtime::TelemetryOptions.new(
      metrics: Temporalio::Runtime::MetricsOptions.new(
        prometheus: Temporalio::Runtime::PrometheusMetricsOptions.new(
          bind_address: '0.0.0.0:9000'
        )
      )
    )
  )
  # @@@SNIPEND
end
