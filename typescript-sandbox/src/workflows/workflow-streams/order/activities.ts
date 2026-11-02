// Each step takes a moment so subscribers can watch the status change
const step = () => new Promise((resolve) => setTimeout(resolve, 200));

export async function validateOrder(_orderId: string): Promise<void> {
  await step();
}

export async function chargePayment(_orderId: string): Promise<void> {
  await step();
}

export async function dispatchOrder(_orderId: string): Promise<void> {
  await step();
}
