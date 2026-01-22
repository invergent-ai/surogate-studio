import { Component, computed, inject, signal } from '@angular/core';
import { Bot, Download, GitBranch, LucideAngularModule, RefreshCw, Trash, Upload } from 'lucide-angular';
import { PageComponent } from '../../shared/components/page/page.component';
import { PageLoadComponent } from '../../shared/components/page-load/page-load.component';
import { MenuItem, PrimeTemplate } from 'primeng/api';
import { TabViewModule } from 'primeng/tabview';
import { TagModule } from 'primeng/tag';
import { injectParams } from 'ngxtension/inject-params';
import { Selectors } from '../../shared/state/selectors';
import { Store } from '@ngxs/store';
import { LayoutService } from '../../shared/service/theme/app-layout.service';
import { TabMenuModule } from 'primeng/tabmenu';
import { hubMenu } from './menu';
import { derivedAsync } from 'ngxtension/derived-async';
import { catchError, tap } from 'rxjs/operators';
import { displayErrorAndRethrow } from '../../shared/util/error.util';
import { injectQueryParams } from 'ngxtension/inject-query-params';
import { LakeFsService } from '../../shared/service/lake-fs.service';
import { ButtonDirective } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DropdownModule } from 'primeng/dropdown';
import { DatePipe, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import dayjs from 'dayjs/esm';
import { CommitDetailsComponent } from './components/commit-details.component';
import { RefSelection, RefSelectorComponent } from './components/ref-selector.component';


@Component({
  selector: 'sm-page-branches',
  standalone: true,
  templateUrl: './commits.page.html',
  imports: [
    LucideAngularModule,
    PageComponent,
    PageLoadComponent,
    PrimeTemplate,
    TabViewModule,
    TagModule,
    TabMenuModule,
    ButtonDirective,
    CardModule,
    DropdownModule,
    FormsModule,
    TableModule,
    DatePipe,
    CommitDetailsComponent,
    RefSelectorComponent
  ],
  styles: `
    .topPanel ::ng-deep  .p-card-body {
      padding: 1rem !important;
    }
  `
})
export class CommitsPage {
  repoId = injectParams('repo');
  ref = injectQueryParams('ref');
  selectedCommit = injectQueryParams('commit');

  repository = computed(() => this.store.selectSignal(Selectors.repositoryById(this.repoId()))());
  branches = derivedAsync(() => this.repository() && this.lakeFsService.listBranches(this.repository().id)
    .pipe(
      tap(branches => {
        let branchToSelect = branches && branches.length > 0 ? branches[0] : null;
        if (this.ref()) {
          const existingBranch = branches.find(b => b.id === this.ref());
          if (existingBranch) {
            branchToSelect = existingBranch;
          }
        }
        this.selectedRef.set({ id: branchToSelect.id, type: 'branch' });
      }),
      catchError((e) => displayErrorAndRethrow(this.store, e))));
  commits = derivedAsync(() => {
    if (this.repository()) {
      let ref = this.ref() ? this.ref() : this.repository().defaultBranch;
      if (this.selectedRef()) {
        ref = this.selectedRef().id;
      }
      return this.lakeFsService.getCommits(this.repository().id, ref)
        .pipe(catchError((e) => displayErrorAndRethrow(this.store, e)))
    }
    return [];
  });

  selectedRef = signal<RefSelection>(null);
  menu: MenuItem[] = hubMenu(this.repoId());

  chooseBranch(ref: RefSelection) {
    this.selectedRef.set(ref);
  }

  refresh() {
  }

  readonly store = inject(Store);
  readonly lakeFsService = inject(LakeFsService);
  readonly layoutService = inject(LayoutService);
  protected readonly Bot = Bot;
  protected readonly RefreshCw = RefreshCw;
  protected readonly dayjs = dayjs;
}
