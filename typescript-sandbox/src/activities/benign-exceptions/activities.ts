// @@@SNIPSTART typescript-benign-exceptions-activity
import {
  ApplicationFailure,
  ApplicationFailureCategory,
} from '@temporalio/common';

export async function myActivity(): Promise<string> {
  try {
    return await callExternalService();
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    throw ApplicationFailure.create({
      message,
      // Mark this error as benign since it's expected
      category: ApplicationFailureCategory.BENIGN,
    });
  }
}
// @@@SNIPEND

// Stands in for a service that isn't ready yet
async function callExternalService(): Promise<string> {
  throw new Error('External service is not ready yet');
}
