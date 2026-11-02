// @@@SNIPSTART typescript-observability-default-logger
import { DefaultLogger, Runtime } from '@temporalio/worker';

const logger = new DefaultLogger('WARN', ({ level, message }) => {
  console.log(`Custom logger: ${level} — ${message}`);
});
Runtime.install({ logger });
// @@@SNIPEND
