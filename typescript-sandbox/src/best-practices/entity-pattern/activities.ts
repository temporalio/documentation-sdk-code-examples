// Records each call so tests can check what the entity did
export const calls: string[] = [];

export async function setup(input: { entityId: string }): Promise<void> {
  calls.push(`setup ${input.entityId}`);
}

export async function runAnActivityOrChildWorkflow(update: { value: number }): Promise<void> {
  calls.push(`update ${update.value}`);
}

export async function cleanup(): Promise<void> {
  calls.push('cleanup');
}
