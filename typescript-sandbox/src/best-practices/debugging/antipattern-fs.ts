// Antipattern: the Worker can't bundle this file as Workflow code
// @@@SNIPSTART typescript-debugging-antipattern-fs
import fs from 'fs';

const config = fs.readFileSync('config.json', 'utf8');
// @@@SNIPEND

export async function configWorkflow(): Promise<string> {
  return config;
}
