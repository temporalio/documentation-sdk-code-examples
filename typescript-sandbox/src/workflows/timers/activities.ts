export const calls: string[] = [];

export async function processOrder(orderProcessingMS: number): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, orderProcessingMS));
  calls.push('processOrder');
}

export async function sendNotificationEmail(): Promise<void> {
  calls.push('sendNotificationEmail');
}

export async function sendReminderEmail(userId: string): Promise<void> {
  calls.push(`sendReminderEmail:${userId}`);
}

export async function somethingElse(): Promise<void> {
  calls.push('somethingElse');
}

export async function complete(): Promise<void> {
  // Takes longer than the 5-second Timer in the antipattern example
  await new Promise((resolve) => setTimeout(resolve, 6000));
  calls.push('complete');
}
