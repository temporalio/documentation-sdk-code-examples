import * as wf from '@temporalio/workflow';
import { Mutex } from 'async-mutex';
import type * as activities from './activities';

const { callGreetingService } = wf.proxyActivities<typeof activities>({
  startToCloseTimeout: '1 minute',
});

// @@@SNIPSTART typescript-message-passing-greeting-workflow
export enum Language {
  ARABIC = 'ARABIC',
  CHINESE = 'CHINESE',
  ENGLISH = 'ENGLISH',
  FRENCH = 'FRENCH',
  HINDI = 'HINDI',
  PORTUGUESE = 'PORTUGUESE',
  SPANISH = 'SPANISH',
}

interface GetLanguagesInput {
  includeUnsupported: boolean;
}

interface ApproveInput {
  name: string;
}

// 👉 Use the objects returned by defineQuery, defineSignal, and defineUpdate to set
// the message handlers in Workflow code, and to send messages from Client code.
export const getLanguages = wf.defineQuery<Language[], [GetLanguagesInput]>('getLanguages');
export const approve = wf.defineSignal<[ApproveInput]>('approve');
export const setLanguage = wf.defineUpdate<Language, [Language]>('setLanguage');
export const setLanguageUsingActivity = wf.defineUpdate<Language, [Language]>('setLanguageUsingActivity');

export async function greetingWorkflow(): Promise<string> {
  const greetings: Partial<Record<Language, string>> = {
    [Language.CHINESE]: '你好，世界',
    [Language.ENGLISH]: 'Hello, world',
  };

  let approvedForRelease = false;
  let approverName: string | undefined;
  let language = Language.ENGLISH;

  wf.setHandler(getLanguages, (input: GetLanguagesInput): Language[] => {
    // 👉 A Query handler returns a value: it must not mutate the Workflow state
    // and can't perform async operations.
    if (input.includeUnsupported) {
      return Object.values(Language);
    } else {
      return Object.keys(greetings) as Language[];
    }
  });

  wf.setHandler(approve, (input) => {
    // 👉 A Signal handler mutates the Workflow state but cannot return a value.
    approvedForRelease = true;
    approverName = input.name;
  });

  wf.setHandler(
    setLanguage,
    (newLanguage: Language) => {
      // 👉 An Update handler can mutate the Workflow state and return a value.
      const previousLanguage = language;
      language = newLanguage;
      return previousLanguage;
    },
    {
      validator: (newLanguage: Language) => {
        // 👉 Update validators are optional
        if (!(newLanguage in greetings)) {
          throw new Error(`${newLanguage} is not supported`);
        }
      },
    },
  );

  const lock = new Mutex();
  wf.setHandler(setLanguageUsingActivity, async (newLanguage) => {
    // 👉 An Update handler can mutate the Workflow state and return a value.
    // 👉 Since this update handler is async, it can execute an activity.
    if (!(newLanguage in greetings)) {
      // 👉 Do the following with the lock held to ensure that multiple calls to setLanguageUsingActivity are processed in order.
      await lock.runExclusive(async () => {
        if (!(newLanguage in greetings)) {
          const greeting = await callGreetingService(newLanguage);
          if (!greeting) {
            // 👉 An update validator cannot be async, so cannot be used to check that the remote
            // callGreetingService supports the requested language. Throwing ApplicationFailure
            // will fail the Update, but the WorkflowExecutionUpdateAccepted event will still be
            // added to history.
            throw new wf.ApplicationFailure(`${newLanguage} is not supported by the greeting service`);
          }
          greetings[newLanguage] = greeting;
        }
      });
    }
    const previousLanguage = language;
    language = newLanguage;
    return previousLanguage;
  });

  await wf.condition(() => approvedForRelease);
  wf.log.info('Approved for release', { approverName });
  return greetings[language]!;
}
// @@@SNIPEND
