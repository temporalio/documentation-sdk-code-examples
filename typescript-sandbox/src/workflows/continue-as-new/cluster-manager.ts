import * as wf from '@temporalio/workflow';
import { AssignNodesToJobUpdateInput, ClusterManagerState, ClusterManagerStateSummary, ClusterState } from './types';

// Keeps track of which job each node in a cluster is assigned to
export class ClusterManager {
  state: ClusterManagerState;
  private maxHistoryLength?: number;

  constructor(input: { state?: ClusterManagerState; testContinueAsNew?: boolean } = {}) {
    this.state = input.state ?? { clusterState: ClusterState.NOT_STARTED, nodes: {} };
    this.maxHistoryLength = input.testContinueAsNew ? 120 : undefined;
  }

  startCluster(): void {
    if (this.state.clusterState !== ClusterState.NOT_STARTED) {
      wf.log.warn(`Cannot start cluster in state ${this.state.clusterState}`);
      return;
    }
    this.state.clusterState = ClusterState.STARTED;
    for (let i = 0; i < 25; i++) {
      this.state.nodes[i.toString()] = null;
    }
  }

  shutdownCluster(): void {
    if (this.state.clusterState !== ClusterState.STARTED) {
      throw new wf.ApplicationFailure(`Cannot shutdown cluster in state ${this.state.clusterState}`);
    }
    this.state.clusterState = ClusterState.SHUTTING_DOWN;
  }

  async assignNodesToJob(input: AssignNodesToJobUpdateInput): Promise<ClusterManagerStateSummary> {
    await wf.condition(() => this.state.clusterState === ClusterState.STARTED);
    const unassignedNodes = Object.keys(this.state.nodes).filter((node) => this.state.nodes[node] === null);
    if (input.numNodes > unassignedNodes.length) {
      throw new wf.ApplicationFailure(
        `Cannot assign ${input.numNodes} nodes; have only ${unassignedNodes.length} available`,
      );
    }
    for (const node of unassignedNodes.slice(0, input.numNodes)) {
      this.state.nodes[node] = input.jobName;
    }
    return this.getStateSummary();
  }

  getState(): ClusterManagerState {
    return { clusterState: this.state.clusterState, nodes: this.state.nodes };
  }

  getStateSummary(): ClusterManagerStateSummary {
    return { assignedNodes: Object.values(this.state.nodes).filter((job) => job !== null).length };
  }

  // @@@SNIPSTART typescript-continue-as-new-should-continue
  shouldContinueAsNew(): boolean {
    if (wf.workflowInfo().continueAsNewSuggested) {
      return true;
    }

    // This is just for ease-of-testing. In production, we trust temporal to tell us when to continue-as-new.
    if (this.maxHistoryLength !== undefined && wf.workflowInfo().historyLength > this.maxHistoryLength) {
      return true;
    }

    return false;
  }
  // @@@SNIPEND
}
