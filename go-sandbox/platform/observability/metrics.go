// Package observability shows how to emit metrics, trace, log, and use Search Attributes with the Go SDK.
package observability

import (
	"log"
	"time"

	"github.com/uber-go/tally/v4"
	"github.com/uber-go/tally/v4/prometheus"
	"go.temporal.io/sdk/client"
	sdktally "go.temporal.io/sdk/contrib/tally"
)

// PrometheusClientOptions returns Client options that expose SDK metrics for Prometheus to scrape.
func PrometheusClientOptions() client.Options {
	// @@@SNIPSTART go-observability-prometheus-metrics
	clientOptions := client.Options{
		MetricsHandler: sdktally.NewMetricsHandler(newPrometheusScope(prometheus.Configuration{
			ListenAddress: "0.0.0.0:9090",
			TimerType:     "histogram",
		})),
	}
	// @@@SNIPEND
	return clientOptions
}

// newPrometheusScope creates a Tally scope that reports to a Prometheus endpoint.
func newPrometheusScope(c prometheus.Configuration) tally.Scope {
	reporter, err := c.NewReporter(prometheus.ConfigurationOptions{
		OnError: func(err error) {
			log.Println("Prometheus reporter error", err)
		},
	})
	if err != nil {
		log.Fatalln("Unable to create Prometheus reporter", err)
	}
	scopeOpts := tally.ScopeOptions{
		CachedReporter:  reporter,
		Separator:       prometheus.DefaultSeparator,
		SanitizeOptions: &sdktally.PrometheusSanitizeOptions,
		Prefix:          "temporal_samples",
	}
	scope, _ := tally.NewRootScope(scopeOpts, time.Second)
	// Convert metric names to the Prometheus naming convention.
	return sdktally.NewPrometheusNamingScope(scope)
}
