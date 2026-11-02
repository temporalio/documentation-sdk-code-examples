import * as wf from '@temporalio/workflow';
import { ClusterManager } from './cluster-manager';
import { AssignNodesToJobUpdateInput, ClusterManagerState, ClusterManagerStateSummary, ClusterState } from './types';

export const startClusterSignal = wf.defineSignal('startCluster');
export const shutdownClusterUpdate = wf.defineUpdate<void>('shutdownCluster');
export const assignNodesToJobUpdate = wf.defineUpdate<ClusterManagerStateSummary, [AssignNodesToJobUpdateInput]>(
  'assignNodesToJob',
);
export const getClusterStatusQuery = wf.defineQuery<ClusterManagerStateSummary>('getClusterStatus');

// @@@SNIPSTART typescript-continue-as-new-workflow
export interface ClusterManagerInput {
  state?: ClusterManagerState;
  testContinueAsNew?: boolean;
}

export async function clusterManagerWorkflow(input: ClusterManagerInput = {}): Promise<ClusterManagerStateSummary> {
  const manager = new ClusterManager(input);
  wf.setHandler(startClusterSignal, () => manager.startCluster());
  wf.setHandler(shutdownClusterUpdate, () => manager.shutdownCluster());
  wf.setHandler(assignNodesToJobUpdate, (update) => manager.assignNodesToJob(update));
  wf.setHandler(getClusterStatusQuery, () => manager.getStateSummary());

  await wf.condition(() => manager.state.clusterState === ClusterState.STARTED);
  await wf.condition(
    () => manager.state.clusterState === ClusterState.SHUTTING_DOWN || manager.shouldContinueAsNew(),
  );
  if (manager.state.clusterState !== ClusterState.SHUTTING_DOWN) {
    // Wait for message handlers to finish before continuing as new
    await wf.condition(wf.allHandlersFinished);
    return await wf.continueAsNew<typeof clusterManagerWorkflow>({
      state: manager.getState(),
      testContinueAsNew: input.testContinueAsNew,
    });
  }
  return manager.getStateSummary();
}
// @@@SNIPEND
