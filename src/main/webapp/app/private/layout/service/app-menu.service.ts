import {Injectable} from '@angular/core';
import {Subject} from 'rxjs';
import {MenuChangeEvent} from './menuchangeevent';
import {LoadAppsAction, LoadDbsAction, LoadVolumesAction} from '../../../shared/state/actions';
import {Store} from '@ngxs/store';

@Injectable({
  providedIn: 'root'
})
export class MenuService {
  private menuSource = new Subject<MenuChangeEvent>();
  private resetSource = new Subject();
  private resetMasterSelection = new Subject();
  private selectedProjectId: string;

  menuSource$ = this.menuSource.asObservable();
  resetSource$ = this.resetSource.asObservable();
  resetMasterSelection$ = this.resetMasterSelection.asObservable();

  constructor(private store: Store) {
  }

  onMenuStateChange(event: MenuChangeEvent) {
    this.menuSource.next(event);
  }

  reset() {
    this.selectedProjectId = null;
    this.resetSource.next(true);
  }

  reload(projectId?: string) {
    if (!this.selectedProjectId) {
      this.loadResources();
      return;
    }

    if (projectId !== this.selectedProjectId) {
      this.reset();
      this.resetSelection(projectId);
      this.selectedProjectId = projectId;
    }
    this.loadResources(projectId);
  }

  resetSelection(id?: string) {
    this.resetMasterSelection.next(id);
  }

  loadResources(projectId?: string) {
    this.selectedProjectId = projectId;
    this.store.dispatch(new LoadAppsAction(projectId));
    this.store.dispatch(new LoadDbsAction(projectId));
    this.store.dispatch(new LoadVolumesAction(projectId));
  }
}
