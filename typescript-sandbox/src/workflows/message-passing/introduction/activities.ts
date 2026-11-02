import type { Language } from './workflows';

// The remote greeting service supports more languages than the Workflow
export async function callGreetingService(toLanguage: Language): Promise<string | undefined> {
  const greetings: Record<Language, string> = {
    ARABIC: 'مرحبا بالعالم',
    CHINESE: '你好，世界',
    ENGLISH: 'Hello, world',
    FRENCH: 'Bonjour, monde',
    HINDI: 'नमस्ते दुनिया',
    PORTUGUESE: 'Olá mundo',
    SPANISH: 'Hola mundo',
  };
  await new Promise((resolve) => setTimeout(resolve, 200)); // Simulate a network call
  return greetings[toLanguage];
}
