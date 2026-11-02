// @@@SNIPSTART typescript-observability-accumulate-logs
import { DefaultLogger, LogEntry } from '@temporalio/worker';

const logs: LogEntry[] = [];
const logger = new DefaultLogger('TRACE', (entry) => logs.push(entry));

logger.debug('hey', { a: 1 });
logger.info('ho');
logger.warn('lets', { a: 1 });
logger.error('go');
// @@@SNIPEND

export { logs };
