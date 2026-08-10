import { 
  DefaultLogger,
  Runtime
} from '@temporalio/worker';
import winston from 'winston';

const winstonLogger = winston.createLogger({
  level: 'debug',
});

const logger = new DefaultLogger('DEBUG', (entry) => {
  winstonLogger.log({
    label: 'worker',
    level: entry.level.toLowerCase(),
    message: entry.message,
    timestamp: Number(entry.timestampNanos / 1_000_000n),
    ...entry.meta,
  });
});

Runtime.install({ logger });