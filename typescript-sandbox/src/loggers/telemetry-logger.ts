import {
  DefaultLogger,
  makeTelemetryFilterString,
  Runtime,
} from '@temporalio/worker';
import winston, { transports } from 'winston';

const logger = winston.createLogger({
  level: 'info',
  format: winston.format.json(),
  transports: [new transports.File({ filename: '/path/to/worker.log' })],
});

Runtime.install({
  logger: new DefaultLogger('INFO', (entry) => {
    logger.log({
      label: entry.meta?.activityId ? 'activity' : entry.meta?.workflowId ? 'workflow' : 'worker',
      level: entry.level.toLowerCase(),
      message: entry.message,
      timestamp: Number(entry.timestampNanos / 1_000_000n),
      ...entry.meta,
    });
  }),
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