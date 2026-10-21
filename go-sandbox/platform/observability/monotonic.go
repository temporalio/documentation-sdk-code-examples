package observability

import (
	"go.opentelemetry.io/otel"
	"go.temporal.io/sdk/client"
	temporalotel "go.temporal.io/sdk/contrib/opentelemetry"
)

// DialWithMonotonicCounters connects a Client whose SDK counters are monotonic OpenTelemetry counters.
func DialWithMonotonicCounters() (client.Client, error) {
	// @@@SNIPSTART go-observability-monotonic-counters
	metricsHandler := temporalotel.NewMetricsHandler(temporalotel.MetricsHandlerOptions{
		Meter:                otel.GetMeterProvider().Meter("temporal-sdk-go"),
		UseMonotonicCounters: true,
	})

	temporalClient, err := client.Dial(client.Options{
		MetricsHandler: metricsHandler,
	})
	// @@@SNIPEND
	return temporalClient, err
}
