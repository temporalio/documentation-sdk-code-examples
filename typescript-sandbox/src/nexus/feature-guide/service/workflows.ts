import { ApplicationFailure } from '@temporalio/workflow';
import { HelloInput, HelloOutput, LanguageCode } from '../api';

export async function helloWorkflow(input: HelloInput): Promise<HelloOutput> {
  return helloWorkflowWithArgs(input.name, input.language);
}

// Takes the name and language as separate arguments
export async function helloWorkflowWithArgs(name: string, language: LanguageCode): Promise<HelloOutput> {
  switch (language) {
    case 'en':
      return { message: `Hello, ${name}!` };
    case 'fr':
      return { message: `Bonjour, ${name}!` };
    case 'de':
      return { message: `Hallo, ${name}!` };
    case 'es':
      return { message: `Hola, ${name}!` };
    case 'tr':
      return { message: `Merhaba, ${name}!` };
    default:
      throw new ApplicationFailure(`Unsupported language: ${language}`);
  }
}
