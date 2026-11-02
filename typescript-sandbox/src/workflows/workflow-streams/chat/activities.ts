// @@@SNIPSTART typescript-workflow-streams-llm-activity
import { Context } from '@temporalio/activity';
import { WorkflowStreamClient } from '@temporalio/workflow-streams/client';
import OpenAI from 'openai';

export interface TextDelta {
  text: string;
}

export interface RetryEvent {
  attempt: number;
}

export async function streamCompletion(prompt: string): Promise<string> {
  const attempt = Context.current().info.attempt;
  await using streamClient = WorkflowStreamClient.fromWithinActivity({
    batchInterval: '200 milliseconds',
  });
  // Disable provider-side retries; let Temporal own retry policy at the Activity layer.
  const openai = new OpenAI({ maxRetries: 0 });

  const deltas = streamClient.topic<TextDelta>('delta');
  const retry = streamClient.topic<RetryEvent>('retry');
  const close = streamClient.topic<Record<string, never>>('close');

  // Tell consumers an earlier attempt's deltas are stale.
  if (attempt > 1) {
    retry.publish({ attempt }, { forceFlush: true });
  }

  const oaiStream = await openai.chat.completions.create({
    model: 'gpt-4o-mini',
    messages: [{ role: 'user', content: prompt }],
    stream: true,
  });

  const full: string[] = [];
  let first = true;
  for await (const chunk of oaiStream) {
    const text = chunk.choices[0]?.delta?.content;
    if (!text) continue;
    // forceFlush only on the first delta so the user sees something
    // immediately; subsequent deltas batch at the 200 ms interval.
    deltas.publish({ text }, first ? { forceFlush: true } : undefined);
    first = false;
    full.push(text);
  }
  close.publish({});
  return full.join('');
}
// @@@SNIPEND
