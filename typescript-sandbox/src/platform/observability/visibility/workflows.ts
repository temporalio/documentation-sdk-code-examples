import { SearchAttributes, upsertSearchAttributes, workflowInfo } from '@temporalio/workflow';

// @@@SNIPSTART typescript-observability-upsert-search-attributes
export async function example(): Promise<SearchAttributes> {
  const customInt = (workflowInfo().searchAttributes.CustomIntField?.[0] as number) || 0;
  upsertSearchAttributes({
    // overwrite the existing CustomIntField: [2]
    CustomIntField: [customInt + 1],

    // delete the existing CustomBoolField: [true]
    CustomBoolField: [],

    // add a new value
    CustomDoubleField: [3.14],
  });
  return workflowInfo().searchAttributes;
}
// @@@SNIPEND

// @@@SNIPSTART typescript-observability-remove-search-attribute
export async function yourWorkflow(): Promise<SearchAttributes> {
  upsertSearchAttributes({ CustomIntField: [1] });

  // ... later, to remove:
  upsertSearchAttributes({ CustomIntField: [] });
  // @@@SNIPEND
  return workflowInfo().searchAttributes;
}
