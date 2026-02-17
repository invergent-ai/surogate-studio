import {createSelector, Selector} from '@ngxs/store';
import {AppState, AppStateModel} from "./app-state";
import {AppStatusWithResources} from '../model/k8s/app-status.model';
import {ILakeFsRepository} from '../model/lakefs.model';

export class Selectors {
  @Selector()
  static hasGlobalError(state: AppStateModel): boolean {
    return !!(state?.globalError && state?.globalError.length > 0);
  }

  @Selector([AppState])
  static projects(state: AppStateModel) {
    return state.projects;
  }

  @Selector([AppState])
  static apps(state: AppStateModel) {
    return state.apps;
  }

  @Selector([AppState])
  static deployments(state: AppStateModel) {
    return {apps: state.apps, dbs: state.dbs, volumes: state.volumes};
  }

  @Selector([AppState])
  static nodes(state: AppStateModel) {
    return state.nodes;
  }

  @Selector([AppState])
  static volumes(state: AppStateModel) {
    return state.volumes;
  }

  @Selector([AppState])
  static zones(state: AppStateModel) {
    return state.zones;
  }

  @Selector([AppState])
  static repositories(state: AppStateModel) {
    return state.repositories;
  }

  @Selector([AppState])
  static repositoriesLoading(state: AppStateModel) {
    return state.repositoriesLoading;
  }

  static repositoryById(repoId: string): (state: AppStateModel) => ILakeFsRepository {
    return createSelector([AppState], (state: AppStateModel) => {
      return state?.repositories ? state.repositories.find(r => r.id === repoId) : null;
    });
  }

  static getEndpointFor(api: string) {
    // Remove leading slash from api if present
    const cleanApi = api.replace(/^\//, '');
    return createSelector([AppState], (state: AppStateModel) => {
      return state?.endpointPrefix ? `${state.endpointPrefix}/${cleanApi}` : `/${cleanApi}`;
    });
  }

  static resourceStatus(appId: string): (state: AppStateModel) => AppStatusWithResources {
    return createSelector([AppState], (state: AppStateModel) => {
      return state?.appStatuses ? state.appStatuses[appId] : null;
    });
  }

  @Selector([AppState])
  static dbs(state: AppStateModel) {
    return state.dbs;
  }

  @Selector([AppState])
  static directLakeFsParams(state: AppStateModel) {
    return state.directLakeFsServiceParams;
  }
}
