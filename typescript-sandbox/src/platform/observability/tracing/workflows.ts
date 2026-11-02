// @@@SNIPSTART typescript-observability-jaeger-propagator
import { propagation } from '@opentelemetry/api';
import { CompositePropagator, W3CBaggagePropagator, W3CTraceContextPropagator } from '@opentelemetry/core';
import { JaegerPropagator } from '@opentelemetry/propagator-jaeger';

propagation.setGlobalPropagator(
  new CompositePropagator({
    propagators: [new W3CTraceContextPropagator(), new W3CBaggagePropagator(), new JaegerPropagator()],
  }),
);
// @@@SNIPEND

export async function tracedWorkflow(): Promise<string> {
  return 'traced';
}
