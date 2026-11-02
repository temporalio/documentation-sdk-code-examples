// @@@SNIPSTART typescript-enriching-ui-current-details
import { getCurrentDetails, setCurrentDetails } from '@temporalio/workflow';

export async function yourWorkflow(input: string): Promise<string> {
  // Get the current details
  const currentDetails = getCurrentDetails();
  console.log(`Current details: ${currentDetails}`);

  // Set/update the current details
  setCurrentDetails('Updated workflow details with new status');

  return 'Workflow completed';
}
// @@@SNIPEND
