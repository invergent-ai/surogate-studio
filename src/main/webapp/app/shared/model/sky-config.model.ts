export interface ISkyConfig {
  name?: string;
  workDir?: string;
  numNodes?: number;
  resources?: IResources;

  envs?: Record<string, string>;
  secrets?: Record<string, string>;
  volumes?: Record<string, string>;
  fileMounts?: Record<string, string>;

  setup?: string;
  run?: string;
  config?: Record<string, string>;
}

export interface IResources {
  infra?: string;
  accelerators?: string;
  acceleratorArgs?: string;
  cpus?: string;
  memory?: string;
  instanceType?: string;
  useSpot?: boolean;
  diskSize?: number;
  diskTier?: string;
  networkTier?: string;
  imageId?: string;
  ports?: string;

  labels?: Record<string, string>;
  autostop?: IAutostop;
  anyOf?: IAnyOf[];
  ordered?: IOrdered[];
  jobRecovery?: string;
}

export interface IAutostop {
  idleMinutes?: number;
  waitFor?: string;
  hook?: string;
  hookTimeout?: number;
}

export interface IAnyOf {
  infra?: string;
  accelerators?: string;
}

export interface IOrdered {
  infra?: string;
}
