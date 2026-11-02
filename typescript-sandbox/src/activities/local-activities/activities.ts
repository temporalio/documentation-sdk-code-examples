export async function getEnvVar(name: string): Promise<string | undefined> {
  return process.env[name];
}
