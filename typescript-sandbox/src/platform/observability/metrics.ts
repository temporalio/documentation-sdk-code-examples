import { makeTelemetryFilterString, Runtime } from '@temporalio/worker';

export function installPrometheusMetrics(): void {
  Runtime.install({
    // @@@SNIPSTART typescript-observability-prometheus
    telemetryOptions: {
      metrics: {
        prometheus: { bindAddress: '0.0.0.0:9464' },
      },
      logging: { filter: makeTelemetryFilterString({ core: 'DEBUG' }), forward: {} },
    },
    // @@@SNIPEND
  });
}
