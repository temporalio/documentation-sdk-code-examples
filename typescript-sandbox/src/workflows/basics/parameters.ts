// @@@SNIPSTART typescript-workflow-basics-parameters
interface ExampleParam {
  name: string;
  born: number;
}
export async function example({ name, born }: ExampleParam): Promise<string> {
  return `Hello ${name}, you were born in ${born}.`;
}
// @@@SNIPEND
