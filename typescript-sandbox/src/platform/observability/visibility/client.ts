import { Client, WorkflowExecutionDescription } from '@temporalio/client';
import { example } from './workflows';

export async function startWithSearchAttributes(client: Client): Promise<WorkflowExecutionDescription['searchAttributes']> {
  // @@@SNIPSTART typescript-observability-set-search-attributes
  const handle = await client.workflow.start(example, {
    taskQueue: 'search-attributes',
    workflowId: 'search-attributes-example-0',
    searchAttributes: {
      CustomIntField: [2],
      CustomKeywordListField: ['keywordA', 'keywordB'],
      CustomBoolField: [true],
      CustomDatetimeField: [new Date()],
      CustomTextField: [
        'String field is for text. When queried, it will be tokenized for partial match. StringTypeField cannot be used in Order By',
      ],
    },
  });

  const { searchAttributes } = await handle.describe();
  // @@@SNIPEND
  await handle.result();
  return searchAttributes;
}
