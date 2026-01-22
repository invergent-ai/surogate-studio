export interface ILakeFsRepository {
  id: string;
  defaultBranch: string;
  creationDate: number;
  metadata: any;
}

export interface ICreateLakeFsRepository extends Omit<ILakeFsRepository, 'creationDate'> {
  description?: string;
}

export interface ILakeFsRef {
  id: string;
  commitId: string;
  metadata: any;
}

export interface ILakeFsBranchCreation {
  name: string;
  source: string;
  force: boolean;
}

export interface ILakeFsTagCreation {
  id: string;
  ref: string;
  force: boolean;
}

export interface ILakeFsCommit {
  id: string;
  committer: string;
  message: string;
  creationDate: number;
  parents: string[];
}

export interface ILakeFsDiff {
  type: 'ADDED' | 'REMOVED' | 'CHANGED' | 'CONFLICT' | 'PREFIX_CHANGED';
  pathType: 'COMMON_PREFIX' | 'OBJECT';
  sizeBytes: number;
  path: string
}

export interface ILakeFsCommit {
  id: string;
  committer: string;
  message: string;
  creationDate: number;
  parents: string[];
}

export interface ILakeFsObjectStats {
  path: string;
  pathType: string;
  checksum: string;
  sizeBytes: number;
  mtime: number;
  metadata: any;
  contentType: string;
}

export interface ILakeFsCommitCreation {
  message: string;
  force?: false;
}

export interface ILakeFsImportJob {
  source: LakeFsImportSource;
  repo: string;
  branch: string;
  token: string;
}

export enum LakeFsRepositoryType {
  MODEL= 'model',
  DATASET = 'dataset',
}

export enum LakeFsImportSource {
  HF = 'hf',
  MODELSCOPE = 'ms'
}

export interface IDirectLakeFsServiceParams {
  endpoint: string;
  auth: string;
  s3Auth: string;
  s3Endpoint: string;
}
