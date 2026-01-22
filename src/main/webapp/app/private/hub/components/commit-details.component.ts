import { Component, computed, inject, input, signal } from '@angular/core';
import { derivedAsync } from 'ngxtension/derived-async';
import { catchError } from 'rxjs/operators';
import { displayErrorAndRethrow } from '../../../shared/util/error.util';
import { LakeFsService } from '../../../shared/service/lake-fs.service';
import { Store } from '@ngxs/store';
import { TableModule, TableRowExpandEvent } from 'primeng/table';
import { Button } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ILakeFsDiff } from '../../../shared/model/lakefs.model';
import { DirectLakeFsService } from '../../../shared/service/direct-lake-fs.service';
import { SideBySideDiffComponent } from 'ngx-diff';

@Component({
  selector: 'sm-commit',
  standalone: true,
  imports: [
    TableModule,
    Button,
    CardModule,
    SideBySideDiffComponent
  ],
  templateUrl: './commit-details.component.html',
  styles: `
    .diffPanel ::ng-deep  .p-card-body {
      padding: 0 !important;
    }
  `
})
export class CommitDetailsComponent {
  commitId = input.required<string>();
  repoId = input.required<string>();

  commit = derivedAsync(() =>
    this.commitId() && this.repoId() && this.lakeFsService.getCommit(this.repoId(), this.commitId())
      .pipe(catchError((e) => displayErrorAndRethrow(this.store, e))));
  diffs = derivedAsync(() =>
    this.commit() && this.lakeFsService.diff(this.repoId(), this.commit().parents[0], this.commit().id)
      .pipe(catchError((e) => displayErrorAndRethrow(this.store, e))));

  selectedDiff = signal<ILakeFsDiff>(null);
  shouldGetLeftStat = computed(() => this.selectedDiff() &&
    (this.selectedDiff().type === 'CHANGED' || this.selectedDiff().type === 'CONFLICT' || this.selectedDiff().type === 'REMOVED'));
  shouldGetRightStat = computed(() => this.selectedDiff() &&
    (this.selectedDiff().type === 'CHANGED' || this.selectedDiff().type === 'CONFLICT' || this.selectedDiff().type === 'ADDED'));
  leftStat = derivedAsync(() => this.shouldGetLeftStat() &&
    this.lakeFsService.getStat(this.repoId(), this.commit().parents[0], this.selectedDiff().path)
      .pipe(catchError((e) => displayErrorAndRethrow(this.store, e))));
  rightStat = derivedAsync(() => this.shouldGetRightStat() &&
    this.lakeFsService.getStat(this.repoId(), this.commit().id, this.selectedDiff().path)
      .pipe(catchError((e) => displayErrorAndRethrow(this.store, e))));
  leftContent = derivedAsync(() => this.leftStat() &&
    this.directLakeFsService.fetchObjectAsText(this.repoId(), this.commit().parents[0], this.selectedDiff().path));
  rightContent = derivedAsync(() => this.rightStat() &&
    this.directLakeFsService.fetchObjectAsText(this.repoId(), this.commit().id, this.selectedDiff().path));

  expandedRows = {};

  showDiff(event: TableRowExpandEvent) {
    this.selectedDiff.set(event.data as ILakeFsDiff);
  }

  readonly directLakeFsService = inject(DirectLakeFsService);
  readonly lakeFsService = inject(LakeFsService);
  readonly store = inject(Store);
}
