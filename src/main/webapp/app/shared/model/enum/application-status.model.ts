// Application status in their normal order
// CREATED -> BUILDING (optional) -> INITIALIZED -> DEPLOYING -> DEPLOYED
export enum ApplicationStatus {
  BUILDING = 'BUILDING',
  CREATED = 'CREATED',
  INITIALIZED = 'INITIALIZED',
  DEPLOYING = 'DEPLOYING',
  DEPLOYED = 'DEPLOYED',
  DELETING = 'DELETING',
  ERROR = 'ERROR'
}

export enum ResourceStatusStage {
  INITIALIZING = 'INITIALIZING', // init containers still running / waiting
  RUNNING = 'RUNNING', // all expected containers running & ready
  WAITING = 'WAITING', // scheduled but not ready (pulling images / readiness not passed)
  RESTARTING = 'RESTARTING', // crash loop without any healthy running containers
  DEGRADED = 'DEGRADED', // some containers running but others waiting / restarting / failed
  COMPLETED = 'COMPLETED', // all containers terminated successfully (phase Succeeded)
  FAILED = 'FAILED', // one or more containers terminated abnormally (phase Failed or crash with no healthy containers)
  UNKNOWN = 'UNKNOWN' // insufficient or unknown information
}

export enum ContainerStatusStage {
  RUNNING = 'RUNNING',
  TERMINATED = 'TERMINATED',
  WAITING = 'WAITING',
  NOT_DEPLOYED = 'NOT DEPLOYED'
}
