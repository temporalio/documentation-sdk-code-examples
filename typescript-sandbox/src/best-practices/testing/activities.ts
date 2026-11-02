export async function makeHTTPRequest(): Promise<string> {
  const response = await fetch('https://httpbin.org/get?answer=42');
  const body = (await response.json()) as { args: { answer: string } };
  return body.args.answer;
}
