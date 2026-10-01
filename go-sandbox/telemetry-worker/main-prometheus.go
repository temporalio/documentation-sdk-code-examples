package telemetryworker

import (
	"go.temporal.io/sdk/client"
	temporalotel "go.temporal.io/sdk/contrib/opentelemetry"
)

func mainPrometheus() {
	// Create the base OTel metrics handler
	metricsHandler := temporalotel.NewMetricsHandler(temporalotel.MetricsHandlerOptions{})

	// Add global/static tags to all emitted metrics
	globalTagsHandler := metricsHandler.WithTags(map[string]string{
		"team": "content-platform",
		"service": "checkout",
		"cost_center": "cc-1042",
		"environment": "production",
	})

	// Attach the tagged handler to client options
	clientOptions := client.Options{
		MetricsHandler: globalTagsHandler,
	}

	temporalClient, err := client.Dial(clientOptions)
	if err != nil {
		panic(err)
	}
	defer temporalClient.Close()
}