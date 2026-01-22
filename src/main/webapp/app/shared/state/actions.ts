import {Message} from "primeng/api";
import {IApplication} from "../model/application.model";
import {INode} from "../model/node.model";
import {AppStatusWithResources} from "../model/k8s/app-status.model";
import { IVolume } from '../model/volume.model';
import { IProject } from '../model/project.model';
import { VmStatus } from '../model/k8s/vm-status.model';
import {DbStatus} from "../model/k8s/db-status.model";
import {IDatabase} from "../model/database.model";
import { ModelStatusWithResources } from '../model/k8s/model-status.model';

export class DisplayGlobalMessageAction {
    static readonly type = '[Global] Add Display Message';
    constructor(public message: Message) {}
}

export class ReportGlobalErrorAction {
    static readonly type = '[Global] Report Error';
    constructor(public errorMessage?: string) {}
}

export class PopulateStoreAction {
  static readonly type = '[Global] Load Initial data';
  constructor(public withResources?: boolean) {}
}

export class LoadZonesAction {
  static readonly type = '[Global] Load Zones';
}

export class LoadProjectsAction {
    static readonly type = '[Global] Load Projects';
}

export class LoadNodesAction {
  static readonly type = '[Global] Load Nodes';
  constructor(public datacenterName?: string) {}
}

export class LoadAppsAction {
  static readonly type = '[Global] Load Apps';
  constructor(public projectId?: string) {}
}

export class LoadDbsAction {
  static readonly type = '[Global] Load DBs';
  constructor(public projectId?: string) {}
}

export class LoadVmsAction {
  static readonly type = '[Global] Load VMs';
  constructor(public projectId?: string) {}
}

export class LoadVolumesAction {
  static readonly type = '[Global] Load Volumes';
  constructor(public projectId?: string) {}
}

export class LoadRepositoriesAction {
  static readonly type = '[Global] Load Repositories';

  constructor() {
  }
}

export class UpdateMessageAction {
  static readonly type = '[Global] Update Project, Apps, VMs, Volumes and Nodes';
  constructor(public message: IUpdateMessage) {}
}

export class AppStatusAction {
  static readonly type = '[Global] App status';
  constructor(public status: AppStatusWithResources) {}
}

export class ModelStatusAction {
  static readonly type = '[Global] Model status';
  constructor(public status: ModelStatusWithResources) {}
}

export class DbStatusAction {
  static readonly type = '[Global] DB status';
  constructor(public status: DbStatus) {}
}

export class VmStatusAction {
  static readonly type = '[Global] VM status';
  constructor(public status: VmStatus) {}
}

export class LogoutAction {
  static readonly type = '[Global] Logout';
  constructor() {}
}

export class IUpdateMessage {
  apps: IApplication[];
  nodes: INode[];
  dbs: IDatabase[];
  volumes: IVolume[];
  projects: IProject[];
  type: UpdateMessageType;
}

export enum UpdateMessageType {
  CREATE = 'CREATE',
  UPDATE = 'UPDATE',
  DELETE = 'DELETE'
}
