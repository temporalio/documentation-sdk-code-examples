import {
  Runtime,
} from '@temporalio/worker';

Runtime.install({
  telemetryOptions: {
    metrics: {
      prometheus: { bindAddress: '0.0.0.0:9464' },
      globalTags: {
        team: 'content-platform',
        service: 'checkout',
        cost_center: 'cc-1042',
        environment: 'production',
      },
    },
  },
});