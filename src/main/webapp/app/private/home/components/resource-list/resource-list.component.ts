import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { NgForOf, NgIf } from '@angular/common';
import { PageLoadComponent } from '../../../../shared/components/page-load/page-load.component';
import { Account } from '../../../../shared/model/account.model';
import { Subject } from 'rxjs';
import { IApplication } from '../../../../shared/model/application.model';
import { Store } from '@ngxs/store';
import { Selectors } from '../../../../shared/state/selectors';
import { takeUntil } from 'rxjs/operators';
import { IDatabase } from '../../../../shared/model/database.model';
import { IVolume } from '../../../../shared/model/volume.model';
import { ResourceCardComponent } from './card/resource-card.component';
import { IVolumeMount } from '../../../../shared/model/volume-mount.model';
import { PanelModule } from 'primeng/panel';
import TranslateDirective from '../../../../shared/language/translate.directive';
import { ApplicationMode } from '../../../../shared/model/enum/application-mode.model';

export interface IResource {
  type: 'app' | 'model' | 'db' | 'vol';
  resource: IApplication | IDatabase | IVolume;
  cpu?: number;
  memory?: number;
  storage?: number;
}

@Component({
  standalone: true,
  selector: 'sm-resource-list',
  templateUrl: './resource-list.component.html',
  imports: [
    ButtonModule,
    NgForOf,
    NgIf,
    PageLoadComponent,
    ResourceCardComponent,
    PanelModule,
    TranslateDirective
  ]
})
export class ResourceListComponent implements OnInit, OnDestroy {
  @Input() user!: Account;

  private destroy$ = new Subject<void>();
  resources: IResource[] = [];

  constructor(private store: Store) {}

  async ngOnInit() {
    this.store.select(Selectors.apps)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (apps) => {
          this.resources = [...this.resources.filter(r => r.type !== 'app' && r.type !== 'model')];
          this.resources.push(...apps.map(resource => {
            const { cpu, memory, storage } = this.computeAppResources(resource);
            return { type: resource.mode === ApplicationMode.MODEL ? 'model' : 'app', resource, cpu, memory, storage };
          }) as IResource[]);
        },
        error: () => {}
      });
    this.store.select(Selectors.dbs)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (dbs) => {
          this.resources = [...this.resources.filter(r => r.type !== 'db')];
          this.resources.push(...dbs.map(resource => {
            return { type: 'db', resource};
          }) as IResource[]);
        },
        error: () => {}
      });
    this.store.select(Selectors.volumes)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (vols) => {
          this.resources = [...this.resources.filter(r => r.type !== 'vol')];
          this.resources.push(...vols.map(resource => {
            return { type: 'vol', resource};
          }) as IResource[]);
        },
        error: () => {}
      });
  }

  computeAppResources(app: IApplication) {
    let cpu = 0;
    let memory = 0;
    let storage = 0;
    if (app.containers) {
      app.containers.forEach(container => {
        cpu += container.cpuLimit;
        memory += +container.memLimit;
        if (container.volumeMounts) {
          container.volumeMounts.forEach(volume => storage += (volume as IVolumeMount).volume.size);
        }
      });

      memory = memory / 1024;
    }

    return { cpu, memory, storage };
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
