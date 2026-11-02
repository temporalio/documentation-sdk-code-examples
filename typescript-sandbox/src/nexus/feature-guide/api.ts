import * as nexus from 'nexus-rpc';

export const helloService = nexus.service('hello', {
  /**
   * Return the input message, unmodified. This Operation is implemented as a
   * Synchronous Nexus Operation.
   */
  echo: nexus.operation<EchoInput, EchoOutput>(),

  /**
   * Return a salutation message, in the requested language. This Operation is
   * implemented by starting the `helloWorkflow` Workflow.
   */
  hello: nexus.operation<HelloInput, HelloOutput>(),
});

// A Service whose Operation starts a Workflow that takes several arguments
export const helloArgsService = nexus.service('hello-args', {
  hello: nexus.operation<HelloInput, HelloOutput>(),
});

export interface EchoInput {
  message: string;
}

export interface EchoOutput {
  message: string;
}

export interface HelloInput {
  name: string;
  language: LanguageCode;
}

export interface HelloOutput {
  message: string;
}

export type LanguageCode = 'en' | 'fr' | 'de' | 'es' | 'tr';
