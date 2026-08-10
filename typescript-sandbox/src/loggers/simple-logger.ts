import {
  DefaultLogger,
  makeTelemetryFilterString,
  Runtime,
} from '@temporalio/worker';

// This is your custom Logger.
const logger = new DefaultLogger('WARN', ({ level, message }) => {
  console.log(`Custom logger: ${level} — ${message}`);
});

Runtime.install({
  logger,
  // The following block is optional, but generally desired.
  // It allows capturing log messages emitted by the underlying Temporal Core SDK (native code).
  // The Telemetry Filter String determine the desired verboseness of messages emitted by the
  // Temporal Core SDK itself ("core"), and by other native libraries ("other").
  telemetryOptions: {
    logging: {
      filter: makeTelemetryFilterString({ core: 'INFO', other: 'INFO' }),
      forward: {},
    },
  },
});