package telemetryworker

import (
	"github.com/uber-go/tally/v4"
	"go.temporal.io/sdk/client"
	sdktally "go.temporal.io/sdk/contrib/tally"
)

func mainOtel() {
    // 1. Create or configure your base Tally scope
    rootScope := tally.NewTestScope("", nil)

    // 2. Add global/default tags to the scope
    taggedScope := rootScope.Tagged(map[string]string{
        "team": "content-platform",
        "service": "checkout",
        "cost_center": "cc-1042",
        "environment": "production",
    })

    // 3. Pass the tagged scope to Temporal's metrics handler
    temporalClient, err := client.Dial(client.Options{
        MetricsHandler: sdktally.NewMetricsHandler(
            sdktally.NewPrometheusNamingScope(taggedScope),
        ),
    })
    
	if err != nil {
		panic(err)
	}

	defer temporalClient.Close()
}