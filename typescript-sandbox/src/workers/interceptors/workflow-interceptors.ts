// @@@SNIPSTART typescript-interceptors-workflow-module
import { workflowInfo } from '@temporalio/workflow';
import { ActivityLogInterceptor } from './activity-log-interceptor';

export const interceptors = () => ({
  outbound: [new ActivityLogInterceptor(workflowInfo().workflowType)],
  inbound: [],
});
// @@@SNIPEND
