import { Component, computed, inject, signal } from '@angular/core';
import { LayoutService } from '../../shared/service/theme/app-layout.service';
import { injectParams } from 'ngxtension/inject-params';
import { PageComponent } from '../../shared/components/page/page.component';
import { PageLoadComponent } from '../../shared/components/page-load/page-load.component';
import { PrivateModule } from '../private.module';
import { CardModule } from 'primeng/card';
import { Store } from '@ngxs/store';
import { Selectors } from '../../shared/state/selectors';
import { DropdownModule } from 'primeng/dropdown';
import { FormsModule } from '@angular/forms';
import { Bot, LucideAngularModule } from 'lucide-angular';
import { TagModule } from 'primeng/tag';
import { TabViewModule } from 'primeng/tabview';
import { HubObjectsComponent } from './components/hub-objects.component';
import { NgIf } from '@angular/common';
import { injectQueryParams } from 'ngxtension/inject-query-params';
import { Router, RouterLink } from '@angular/router';
import { TabMenuModule } from 'primeng/tabmenu';
import { MenuItem } from 'primeng/api';
import { hubMenu } from './menu';
import { ButtonDirective } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { ChipsModule } from 'primeng/chips';
import { LakeFsService } from '../../shared/service/lake-fs.service';
import { lastValueFrom } from 'rxjs';
import { LoadRepositoriesAction } from '../../shared/state/actions';
import {repoDisplayNameOrId} from "../../shared/util/naming.util";

@Component({
  standalone: true,
  templateUrl: './repo.page.html',
  imports: [
    PageComponent,
    PageLoadComponent,
    PrivateModule,
    CardModule,
    DropdownModule,
    FormsModule,
    LucideAngularModule,
    TagModule,
    TabViewModule,
    HubObjectsComponent,
    NgIf,
    RouterLink,
    TabMenuModule,
    ButtonDirective,
    DialogModule,
    ChipsModule
  ]
})
export class RepoPage {
  repoId = injectParams('repo');
  ref = injectQueryParams('ref');
  refType = injectQueryParams<'branch' | 'tag'>('refType');
  repository = computed(() => this.store.selectSignal(Selectors.repositoryById(this.repoId()))());
  repositoryType = computed(() => this.repository() && this.repository().metadata.type);
  menu: MenuItem[] = hubMenu(this.repoId());
  deleteRepoVisible = signal(false);
  confirmRepoId = signal('');
  deleteRepoLoading = signal(false);

  async deleteRepository() {
    try {
      this.deleteRepoLoading.set(true);
      await lastValueFrom(this.lakeFsService.deleteRepository(this.repoId()));
      this.deleteRepoVisible.set(false);
      this.store.dispatch([new LoadRepositoriesAction()]);
      await this.router.navigate(['/', 'hub'], {queryParams: {type: this.repositoryType()}});
    } finally {
      this.deleteRepoLoading.set(false);
    }
  }

  getRepoName() {
    return repoDisplayNameOrId(this.repository());
  }

  readonly store = inject(Store);
  readonly layoutService = inject(LayoutService);
  readonly lakeFsService = inject(LakeFsService);
  readonly router = inject(Router);

  protected readonly Bot = Bot;
}
