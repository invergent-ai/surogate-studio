import { Component, computed, inject, input, signal } from '@angular/core';
import { TextRendererComponent } from './renderers/text-renderer.component';
import { NgIf } from '@angular/common';
import { derivedAsync } from 'ngxtension/derived-async';
import { catchError, map } from 'rxjs/operators';
import { displayErrorAndRethrow } from '../../../shared/util/error.util';
import { Store } from '@ngxs/store';
import { of } from 'rxjs';
import { ButtonDirective } from 'primeng/button';
import { LakeFsService } from '../../../shared/service/lake-fs.service';

@Component({
  selector: 'sm-lazy-text-viewer',
  standalone: true,
  imports: [TextRendererComponent, NgIf, ButtonDirective],
  template: `
    <sm-text-renderer [data]="visibleText()"></sm-text-renderer>

    <div class="actions" *ngIf="hasMore()">
      <button pButton type="submit" size="small" (click)="loadMore()">Load more</button>
    </div>
  `,
  styles: [
    `
      .actions {
        text-align: center;
      }
    `,
  ],
})
export class LazyTextViewerComponent {
  repository = input<any>();
  ref = input<string>();
  object = input<any>();
  preloadedText = input<string | null>(null);
  CHUNK_SIZE = 10000;

  objectTextData = derivedAsync<string>(() => {
    const preloaded = this.preloadedText();
    if (preloaded !== null) {
      return of(preloaded);
    }
    const repo = this.repository();
    const ref = this.ref();
    const obj = this.object();
    if (!repo || !ref || !obj) return of('');
    return this.lakeFsService.fetchObjectAsText(repo.id, ref, obj.path).pipe(
      map(text => text ?? ''),
      catchError(e => {
        displayErrorAndRethrow(this.store, e);
        return of('');
      }),
    );
  });

  visibleLength = signal(this.CHUNK_SIZE);

  fullText = computed<string>(() => this.objectTextData() ?? '');

  visibleText = computed(() => this.fullText().substring(0, this.visibleLength()));

  loadMore() {
    this.visibleLength.update(len => len + this.CHUNK_SIZE);
  }

  hasMore() {
    return this.visibleLength() < this.fullText()?.length;
  }

  readonly lakeFsService = inject(LakeFsService);
  readonly store = inject(Store);
}
