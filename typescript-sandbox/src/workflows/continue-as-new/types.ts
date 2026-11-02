export enum ClusterState {
  NOT_STARTED = 'NOT_STARTED',
  STARTED = 'STARTED',
  SHUTTING_DOWN = 'SHUTTING_DOWN',
}

export interface ClusterManagerState {
  clusterState: ClusterState;
  // A plain object rather than a Map, so the state survives serialization when passed to the next run
  nodes: Record<string, string | null>;
}

export interface ClusterManagerStateSummary {
  assignedNodes: number;
}

export interface AssignNodesToJobUpdateInput {
  numNodes: number;
  jobName: string;
}
