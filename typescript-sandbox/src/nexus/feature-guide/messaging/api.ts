import * as nexus from 'nexus-rpc';

export const nexusGreetingService = nexus.service('NexusGreetingService', {
  /**
   * Returns the list of all known languages for the given user's entity workflow.
   */
  getLanguages: nexus.operation<GetLanguagesInput, GetLanguagesOutput>(),
});

export type Language = 'arabic' | 'chinese' | 'english' | 'french' | 'hindi' | 'portuguese' | 'spanish';

export interface GetLanguagesInput {
  userId: string;
}

export type GetLanguagesOutput = Language[];
