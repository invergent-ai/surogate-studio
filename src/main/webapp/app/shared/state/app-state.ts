import {Injectable} from '@angular/core';
import {Action, State, StateContext} from '@ngxs/store';
import {enableMapSet, produce} from 'immer';
import {
  AppStatusAction,
  LoadAppsAction,
  LoadDbsAction,
  LoadNodesAction,
  LoadProjectsAction,
  LoadRepositoriesAction,
  LoadVolumesAction,
  LoadZonesAction,
  LogoutAction,
  ModelStatusAction,
  PopulateStoreAction,
  ReportGlobalErrorAction,
  UpdateMessageAction,
  UpdateMessageType
} from './actions';
import {IApplication} from '../model/application.model';
import {INode} from '../model/node.model';
import {IVolume} from '../model/volume.model';
import {IProject} from '../model/project.model';
import {IDatabase} from '../model/database.model';
import {ProjectService} from '../service/project.service';
import {lastValueFrom} from 'rxjs';
import {NodeService} from '../service/node.service';
import {ApplicationService} from '../service/application.service';
import {DatabaseService} from '../service/database.service';
import {VolumeService} from '../service/volume.service';
import {IBaseEntity} from '../model/base-entity.model';
import {IZone} from '../model/zone.model';
import {map} from 'rxjs/operators';
import {HttpResponse} from '@angular/common/http';
import {ZoneService} from '../service/zone.service';
import {environment} from 'environments/environment';
import {isEqual} from 'lodash';
import {AppStatusWithResources} from '../model/k8s/app-status.model';
import {IDirectLakeFsServiceParams, ILakeFsRepository} from '../model/lakefs.model';
import {LakeFsService} from '../service/lake-fs.service';

export const DEFAULT_QUERY = {
  page: 0,
  size: 100,
  sort: [] as any,
  criteria: [] as any
};

export interface AppStateModel {
  globalError: string;
  apps: IApplication[];
  appsLoading: boolean;
  models: IApplication[];
  modelsLoading: boolean;
  nodes: INode[];
  nodesLoading: boolean;
  dbs: IDatabase[];
  dbsLoading: boolean;
  volumes: IVolume[];
  volumesLoading: boolean;
  projects: IProject[];
  projectsLoading: boolean;
  endpointPrefix: string;
  zones: IZone[];
  zonesLoading: boolean;
  appStatuses: Record<string, AppStatusWithResources>;
  repositories: ILakeFsRepository[];
  repositoriesLoading: boolean;
  directLakeFsServiceParams: IDirectLakeFsServiceParams;
}

@State<AppStateModel>({
  name: 'app',
  defaults: {
    globalError: null,
    apps: [],
    appsLoading: true,
    models: [],
    modelsLoading: true,
    nodes: [],
    nodesLoading: true,
    dbs: [],
    dbsLoading: true,
    volumes: [],
    volumesLoading: true,
    projects: [],
    projectsLoading: true,
    endpointPrefix: environment.SERVER_API_URL.replace(/\/$/, ''),
    zones: [],
    zonesLoading: true,
    appStatuses: {},
    repositories: [],
    repositoriesLoading: false,
    directLakeFsServiceParams: null
  }
})
@Injectable()
export class AppState {
  constructor(
    private projectService: ProjectService,
    private nodeService: NodeService,
    private applicationService: ApplicationService,
    private databaseService: DatabaseService,
    private volumeService: VolumeService,
    private zoneService: ZoneService,
    private lakeFsService: LakeFsService
  ) {
    enableMapSet();
  }

  @Action(LogoutAction)
  async logout(ctx: StateContext<AppStateModel>, _: LogoutAction) {
    ctx.patchState({
      globalError: null,
      apps: [],
      nodes: [],
      dbs: [],
      volumes: [],
      projects: []
    });
  }

  @Action(AppStatusAction)
  updateAppStatus(ctx: StateContext<AppStateModel>, action: AppStatusAction) {
    const state = ctx.getState();

    if (state.apps) {
      if (!isEqual(state.appStatuses[action.status.applicationId], action.status)) {
        ctx.setState(produce(draft => {
          draft.appStatuses[action.status.applicationId] = action.status;
        }));
      }

      const app = state.apps.find(app => app.id === action.status.applicationId);
      if (app && !isEqual(action.status.status, app.status)) {
        const toUpdate: IApplication = {
          ...app,
          status: action.status.status
        };

        ctx.setState(produce(draft => {
          draft.apps = state.apps.map(app => app.id === toUpdate.id ? toUpdate : app);
        }));
      }
    }
  }

  @Action(ModelStatusAction)
  updateModelStatus(ctx: StateContext<AppStateModel>, action: ModelStatusAction) {
    const state = ctx.getState();
    if (state.apps) {
      if (action.status.router) {
        if (!isEqual(state.appStatuses[action.status.router.applicationId], action.status.router)) {
          ctx.setState(produce(draft => {
            draft.appStatuses[action.status.router.applicationId] = action.status.router;
          }));
        }
      }
      if (action.status.worker) {
        if (!isEqual(state.appStatuses[action.status.worker.applicationId], action.status.worker)) {
          ctx.setState(produce(draft => {
            draft.appStatuses[action.status.worker.applicationId] = action.status.worker;
          }));
        }
      }
      if (action.status.cache) {
        if (!isEqual(state.appStatuses[action.status.cache.applicationId], action.status.cache)) {
          ctx.setState(produce(draft => {
            draft.appStatuses[action.status.cache.applicationId] = action.status.cache;
          }));
        }
      }
    }
  }

  @Action(LoadProjectsAction)
  async loadProjects(ctx: StateContext<AppStateModel>, action: LoadProjectsAction) {
    ctx.patchState({ projectsLoading: true });
    try {
      const projects = await lastValueFrom(this.projectService.query(DEFAULT_QUERY)
        .pipe(
          map((res: HttpResponse<IProject[]>) => res.body ?? []),
          map(projects => projects.sort((n1, n2) => n1.name.localeCompare(n2.name)))
        ));
      ctx.patchState({ projects });
    } catch (error) {
      ctx.dispatch(new ReportGlobalErrorAction(`Error loading projects: ${error}`));
      ctx.patchState({ projects: [] });
    } finally {
      ctx.patchState({ projectsLoading: false });
    }
  }

  @Action(LoadAppsAction)
  async loadApps(ctx: StateContext<AppStateModel>, action: LoadAppsAction) {
    ctx.patchState({ appsLoading: true });
    try {
      const query = { ...DEFAULT_QUERY };
      if (action.projectId) {
        query.criteria = [{ key: 'projectId.equals', value: action.projectId }];
      }
      const apps = await lastValueFrom(this.applicationService.query(query)
        .pipe(
          map((res: HttpResponse<IApplication[]>) => res.body ?? []),
          map(apps => apps.sort((n1, n2) => n1.name.localeCompare(n2.name)))
        ));
      ctx.patchState({ apps });
    } catch (error) {
      ctx.dispatch(new ReportGlobalErrorAction(`Error loading applications: ${error}`));
      ctx.patchState({ apps: [] });
    } finally {
      ctx.patchState({ appsLoading: false });
    }
  }

  @Action(LoadDbsAction)
  async loadDbs(ctx: StateContext<AppStateModel>, action: LoadDbsAction) {
    ctx.patchState({ dbsLoading: true });
    try {
      const query = { ...DEFAULT_QUERY };
      if (action.projectId) {
        query.criteria = [{ key: 'projectId.equals', value: action.projectId }];
      }
      const dbs = await lastValueFrom(this.databaseService.query(query)
        .pipe(
          map((res: HttpResponse<IDatabase[]>) => res.body ?? []),
          map(dbs => dbs.sort((n1, n2) => n1.name.localeCompare(n2.name)))
        ));
      ctx.patchState({ dbs });
    } catch (error) {
      ctx.dispatch(new ReportGlobalErrorAction(`Error loading databases: ${error}`));
      ctx.patchState({ dbs: [] });
    } finally {
      ctx.patchState({ dbsLoading: false });
    }
  }

  @Action(LoadVolumesAction)
  async loadVolumes(ctx: StateContext<AppStateModel>, action: LoadVolumesAction) {
    ctx.patchState({ volumesLoading: true });
    try {
      const query = { ...DEFAULT_QUERY };
      if (action.projectId) {
        query.criteria = [{ key: 'projectId.equals', value: action.projectId }];
      }
      const volumes = await lastValueFrom(this.volumeService.query(query)
        .pipe(
          map((res: HttpResponse<IVolume[]>) => res.body ?? []),
          map(volumes => volumes.sort((n1, n2) => n1.name.localeCompare(n2.name)))
        ));
      ctx.patchState({ volumes });
    } catch (error) {
      ctx.dispatch(new ReportGlobalErrorAction(`Error loading volumes: ${error}`));
      ctx.patchState({ volumes: [] });
    } finally {
      ctx.patchState({ volumesLoading: false });
    }
  }

  @Action(LoadNodesAction)
  async loadNodes(ctx: StateContext<AppStateModel>, action: LoadNodesAction) {
    ctx.patchState({ nodesLoading: true });
    try {
      const query = { ...DEFAULT_QUERY };
      if (action.datacenterName) {
        query.criteria = [{ key: 'datacenterName.equals', value: action.datacenterName }];
      }
      const nodes = await lastValueFrom(this.nodeService.query(query)
        .pipe(
          map((res: HttpResponse<INode[]>) => res.body ?? []),
          map(nodes => nodes.sort((n1, n2) => n1.name.localeCompare(n2.name)))
        ));
      ctx.patchState({ nodes });
    } catch (error) {
      ctx.dispatch(new ReportGlobalErrorAction(`Error loading nodes: ${error}`));
      ctx.patchState({ nodes: [] });
    } finally {
      ctx.patchState({ nodesLoading: false });
    }
  }

  @Action(LoadZonesAction)
  async loadZones(ctx: StateContext<AppStateModel>, action: LoadZonesAction) {
    ctx.patchState({ zonesLoading: true });
    try {
      let zones = await lastValueFrom(this.zoneService.query()
        .pipe(
          map((res: HttpResponse<IZone[]>) => res.body ?? []),
          map(zones => zones.sort((z1, z2) => z1.name.localeCompare(z2.name)))
        ));
      ctx.patchState({ zones: zones });
    } catch (error) {
      ctx.dispatch(new ReportGlobalErrorAction(`Error loading zones: ${error}`));
      ctx.patchState({ zones: [] });
    } finally {
      ctx.patchState({ zonesLoading: false });
    }
  }

  @Action(LoadRepositoriesAction)
  async loadRepositories(ctx: StateContext<AppStateModel>, action: LoadRepositoriesAction) {
    ctx.patchState({ repositoriesLoading: true });
    try {
      let repositories = await lastValueFrom(this.lakeFsService.listRepositories()
        .pipe(
          map(repos => repos.sort((r1, r2) => r1.id.localeCompare(r2.id)))
        ));
      ctx.patchState({ repositories });
    } catch (error) {
      ctx.dispatch(new ReportGlobalErrorAction(`Error loading repositories: ${error}`));
      ctx.patchState({ repositories: [] });
    }

    try {
      let directLakeFsServiceParams = await lastValueFrom(this.lakeFsService.getDirectServiceParams());
      ctx.patchState({ directLakeFsServiceParams });
    } catch (error) {
      ctx.dispatch(new ReportGlobalErrorAction(`Error loading lakefs config: ${error}`));
      ctx.patchState({ directLakeFsServiceParams: null });
    } finally {
      ctx.patchState({ repositoriesLoading: false });
    }
  }

  @Action(UpdateMessageAction)
  processMessage(ctx: StateContext<AppStateModel>, action: UpdateMessageAction) {
    const sortByNameAsc = (a: any, b: any): number => a.name.localeCompare(b.name);

    if (action.message.projects) {
      ctx.setState(produce(draft => {
        draft.projects = this.merge(draft.projects, action.message.projects, action.message.type)
          .sort(sortByNameAsc) as IProject[];
      }));
    }
    if (action.message.apps) {
      ctx.setState(produce(draft => {
        draft.apps = this.merge(draft.apps, action.message.apps, action.message.type)
          .sort(sortByNameAsc) as IApplication[];
      }));
    }
    if (action.message.dbs) {
      ctx.setState(produce(draft => {
        draft.dbs = this.merge(draft.dbs, action.message.dbs, action.message.type)
          .sort(sortByNameAsc) as IDatabase[];
      }));
    }
    if (action.message.volumes) {
      ctx.setState(produce(draft => {
        draft.volumes = this.merge(draft.volumes, action.message.volumes, action.message.type)
          .sort(sortByNameAsc) as IVolume[];
      }));
    }
    if (action.message.nodes) {
      ctx.setState(produce(draft => {
        draft.nodes = this.merge(draft.nodes, action.message.nodes, action.message.type)
          .sort(sortByNameAsc) as INode[];
      }));
    }
  }

  @Action(PopulateStoreAction)
  populateStore(ctx: StateContext<AppStateModel>, action: PopulateStoreAction) {
    ctx.dispatch([
      new LoadZonesAction(),
      new LoadProjectsAction(),
      new LoadNodesAction(),
      ...(action.withResources ? [
        new LoadAppsAction(),
        new LoadDbsAction(),
        new LoadVolumesAction(),
        new LoadRepositoriesAction()
      ] : [])
    ]);
  }

  private merge(existing: IBaseEntity[],
                fromMessage: IBaseEntity[],
                type: UpdateMessageType): IBaseEntity[] {
    switch (type) {
      case UpdateMessageType.CREATE: {
        const existingIds = existing.map(obj => obj.id);
        const newObjs = fromMessage.filter(obj => existingIds.indexOf(obj.id) < 0);
        return [...existing, ...newObjs];
      }
      case UpdateMessageType.UPDATE: {
        const toUpdateIds = fromMessage.map(obj => obj.id);
        const merged = existing.map(obj =>
          toUpdateIds.indexOf(obj.id) < 0 ? obj :
            fromMessage.filter(eobj => eobj.id == toUpdateIds[toUpdateIds.indexOf(obj.id)])[0]);
        return [...merged];
      }
      case UpdateMessageType.DELETE: {
        const toDeleteIds = fromMessage.map(obj => obj.id);
        const remaining = existing.filter(obj => toDeleteIds.indexOf(obj.id) < 0);
        return [...remaining];
      }
      default: {
        return [...existing];
      }
    }
  }
}
