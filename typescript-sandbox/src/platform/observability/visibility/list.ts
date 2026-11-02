import { Connection } from '@temporalio/client';

export async function listRunning(
  connectionOptions?: Parameters<typeof Connection.connect>[0],
): ReturnType<Connection['workflowService']['listWorkflowExecutions']> {
  // @@@SNIPSTART typescript-observability-list-workflows
  const connection = await Connection.connect(connectionOptions);
  const response = await connection.workflowService.listWorkflowExecutions({
    namespace: 'default',
    query: `ExecutionStatus = "Running"`,
  });
  // @@@SNIPEND
  await connection.close();
  return response;
}
