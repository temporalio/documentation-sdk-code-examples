package observability

import (
	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/contrib/datadog/tracing"
	"go.temporal.io/sdk/contrib/opentelemetry"
	"go.temporal.io/sdk/contrib/opentracing"
	"go.temporal.io/sdk/interceptor"
)

// @@@SNIPSTART go-observability-tracing-interceptors
// OpenTelemetry
func newOpenTelemetryInterceptor() (interceptor.Interceptor, error) {
	return opentelemetry.NewTracingInterceptor(opentelemetry.TracerOptions{})
}

// OpenTracing
func newOpenTracingInterceptor() (interceptor.Interceptor, error) {
	return opentracing.NewInterceptor(opentracing.TracerOptions{})
}

// Datadog
func newDatadogInterceptor() interceptor.Interceptor {
	return tracing.NewTracingInterceptor(tracing.TracerOptions{})
}
// @@@SNIPEND

// DialWithTracing connects a Client that traces Workflows and Activities with OpenTelemetry.
func DialWithTracing() (client.Client, error) {
	tracingInterceptor, err := newOpenTelemetryInterceptor()
	if err != nil {
		return nil, err
	}
	// @@@SNIPSTART go-observability-register-tracing
	c, err := client.Dial(client.Options{
		Interceptors: []interceptor.ClientInterceptor{tracingInterceptor},
	})
	// @@@SNIPEND
	return c, err
}
