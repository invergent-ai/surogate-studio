import {Component, computed, effect, inject, input, OnDestroy, OnInit, signal, untracked} from '@angular/core';
import { DuckDbService } from '../../../../shared/service/duckdb.service';
import { lastValueFrom, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ILakeFsObjectStats } from '../../../../shared/model/lakefs.model';
import { getFileExtension } from '../file-renderer.component';
import { derivedAsync } from 'ngxtension/derived-async';
import { displayError } from '../../../../shared/util/error.util';
import { Store } from '@ngxs/store';
import { NgForOf, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CodeEditor } from '@acrodata/code-editor';
import { languages } from '@codemirror/language-data';
import { ButtonDirective } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { LakeFsService } from '../../../../shared/service/lake-fs.service';

@Component({
  selector: 'sm-duckdb-renderer',
  standalone: true,
  imports: [NgForOf, FormsModule, CodeEditor, ButtonDirective, NgIf, SkeletonModule],
  template: `
    <div class="flex flex-column gap-4">
      <div>
        <code-editor language="SQL" [(ngModel)]="code" [languages]="languages" placeholder="DuckDB SQL..."></code-editor>
        <div class="flex justify-content-end">
          <p class="font-italic text-xs">
            Powered by <a href="https://duckdb.org/2021/10/29/duckdb-wasm.html" target="_blank">DuckDB-WASM</a>. For a full SQL reference,
            see the <a href="https://duckdb.org/docs/stable/sql/statements/select.html" target="_blank">DuckDB Documentation</a>
          </p>
        </div>
      </div>
      <button pButton severity="success" size="small" label="Execute" (click)="runQuery()" [disabled]="isLoading()"></button>
      <div *ngIf="isLoading()">
        <table class="table w-full" style="border-collapse: collapse">
          <thead>
            <tr>
              <th *ngFor="let i of [0, 1, 2, 3]" class="surface-100 p-1"><p-skeleton></p-skeleton></th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let i of [0, 1, 2, 3, 4]">
              <td *ngFor="let j of [0, 1, 2, 3]" class="p-1"><p-skeleton></p-skeleton></td>
            </tr>
          </tbody>
        </table>
      </div>
      <table *ngIf="!isLoading()" class="table table-striped w-full" style="border-collapse: collapse">
        <thead>
          <th *ngFor="let col of columns()" style="text-align: left; border-collapse: collapse" class="surface-100 p-1 border-1">
            {{ col }}
          </th>
        </thead>
        <tbody>
          <tr *ngFor="let row of queryResults()">
            <td *ngFor="let col of columns()" style="text-align: left; border-collapse: collapse" class="border-1 p-1">{{ row[col] }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  `,
})
export class DuckDBRendererComponent implements OnInit, OnDestroy {
  repoId = input.required<string>();
  refId = input.required<string>();
  object = input.required<ILakeFsObjectStats>();

  duckDbReady = signal(false);
  code = signal<string>('');
  queryToRun = signal<string>('');

  initialQuery = computed(() => {
    if (this.object() && this.refId() && this.repoId()) {
      const fileExtension = getFileExtension(this.object().path);
      if (fileExtension === 'csv') {
        return `SELECT * FROM READ_CSV('lakefs://${this.repoId()}/${this.refId()}/${this.object().path}', AUTO_DETECT = TRUE) LIMIT 20`;
      } else if (fileExtension === 'tsv') {
        return `SELECT *  FROM READ_CSV('lakefs://${this.repoId()}/${this.refId()}/${this.object().path}', DELIM='\t', AUTO_DETECT=TRUE) LIMIT 20`;
      } else if (fileExtension === 'parquet') {
        return `SELECT * FROM READ_PARQUET('lakefs://${this.repoId()}/${this.refId()}/${this.object().path}') LIMIT 20`;
      } else if (fileExtension === 'arrow') {
        return `SELECT * FROM READ_ARROW('lakefs://${this.repoId()}/${this.refId()}/${this.object().path}') LIMIT 20`;
      }
    }
    return null;
  });
  queryResults = signal<any[]>([]);
  columns = computed(() => (this.queryResults().length ? Object.keys(this.queryResults()[0]) : []));
  isLoading = signal(true);

  store = inject(Store);
  duckDbService = inject(DuckDbService);
  lakeFsService = inject(LakeFsService);
  destroy$ = new Subject<void>();

  constructor() {
    effect(
      () => {
        const initial = this.initialQuery();
        if (initial) {
          this.code.set(initial);
          this.queryToRun.set(initial);
        }
      },
      { allowSignalWrites: true },
    );
  }

  ngOnInit() {
    this.duckDbService.isReady$.pipe(takeUntil(this.destroy$)).subscribe(async isReady => {
      this.duckDbReady.set(isReady);
      if (isReady) {
        try {
          await lastValueFrom(this.lakeFsService.loadS3Config());
        } catch (e) {
          displayError(this.store, 'Failed to load S3 config');
          return;
        }
        await this.runQuery();
      }
    });
  }

  async runQuery() {
    if (!this.duckDbReady() || !this.code()) return;
    this.isLoading.set(true);

    // Allow UI to update (button disable) before heavy work
    // the DuckDB WASM query blocks the UI thread, so the disabled state cannot paint before execution finishes
    await Promise.resolve();

    try {
      const res = await this.duckDbService.query(this.code());
      this.queryResults.set(res);
    } catch (e: any) {
      displayError(this.store, `Query error: ${e.message}`);
      this.queryResults.set([]);
    } finally {
      this.isLoading.set(false);
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  protected readonly languages = languages;
}
