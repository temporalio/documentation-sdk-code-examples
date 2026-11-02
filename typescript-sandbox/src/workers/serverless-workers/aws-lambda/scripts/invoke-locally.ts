import type { Context } from 'aws-lambda';
import { handler } from '../index';

// Invokes the handler once, as the Lambda runtime would, with a deadline INVOCATION_MS from now
async function invoke(): Promise<void> {
  const deadline = Date.now() + Number(process.env.INVOCATION_MS ?? 15_000);
  const context = {
    awsRequestId: 'local-invocation',
    functionName: 'local',
    getRemainingTimeInMillis: () => deadline - Date.now(),
  } as Context;
  await handler({}, context);
  console.log('Invocation finished');
}

invoke().catch((err) => {
  console.error(err);
  process.exit(1);
});
