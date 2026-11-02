import { Client, WorkflowUpdateStage } from '@temporalio/client';
import { approve, getLanguages, greetingWorkflow, Language, setLanguage } from './workflows';

export async function runGreetingClient(client: Client, taskQueue: string): Promise<string> {
  // @@@SNIPSTART typescript-message-passing-start
  const handle = await client.workflow.start(greetingWorkflow, {
    taskQueue,
    workflowId: 'greeting-workflow',
  });
  // @@@SNIPEND

  // @@@SNIPSTART typescript-message-passing-send-query
  const supportedLanguages = await handle.query(getLanguages, {
    includeUnsupported: false,
  });
  // @@@SNIPEND
  console.log(`Supported languages: ${supportedLanguages}`);

  // @@@SNIPSTART typescript-message-passing-execute-update
  let previousLanguage = await handle.executeUpdate(setLanguage, {
    args: [Language.CHINESE],
  });
  // @@@SNIPEND
  console.log(`Language changed: ${previousLanguage} -> ${Language.CHINESE}`);

  // @@@SNIPSTART typescript-message-passing-start-update
  const updateHandle = await handle.startUpdate(setLanguage, {
    args: [Language.ENGLISH],
    waitForStage: WorkflowUpdateStage.ACCEPTED,
  });
  previousLanguage = await updateHandle.result();
  // @@@SNIPEND
  console.log(`Language changed: ${previousLanguage} -> ${Language.ENGLISH}`);

  // @@@SNIPSTART typescript-message-passing-send-signal
  await handle.signal(approve, { name: 'me' });
  // @@@SNIPEND

  return await handle.result();
}
