let calls = 0;

// Each call returns a matching x and y, so a Workflow that ends up with x !== y mixed results from two calls
export async function myActivity(): Promise<{ x: number; y: number }> {
  calls++;
  return { x: calls, y: calls };
}
