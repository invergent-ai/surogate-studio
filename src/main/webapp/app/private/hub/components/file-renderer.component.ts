import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { ILakeFsObjectStats, ILakeFsRepository } from '../../../shared/model/lakefs.model';
import { DuckDBRendererComponent } from './renderers/duckdb-renderer.component';
import { PdfRendererComponent } from './renderers/pdf-renderer.component';
import { UnsupportedRendererComponent } from './renderers/unsupported-renderer.component';
import { MarkdownRendererComponent } from './renderers/markdown-renderer.component';
import { IpynbRendererComponent } from './renderers/ipynb-renderer.component';
import { ImageRendererComponent } from './renderers/image-renderer.component';
import { ToolargeRendererComponent } from './renderers/toolarge-renderer.component';
import { Store } from '@ngxs/store';
import { LazyTextViewerComponent } from './lazy-text-viewer';
import { LakeFsService } from '../../../shared/service/lake-fs.service';
import { ProgressBarModule } from 'primeng/progressbar';

const MAX_FILE_SIZE = 100 * 5 * 1024 * 1024; // 500MB

@Component({
  selector: 'sm-file-renderer',
  standalone: true,
  templateUrl: './file-renderer.component.html',
  imports: [
    DuckDBRendererComponent,
    PdfRendererComponent,
    UnsupportedRendererComponent,
    MarkdownRendererComponent,
    IpynbRendererComponent,
    ImageRendererComponent,
    ToolargeRendererComponent,
    LazyTextViewerComponent,
    ProgressBarModule,
  ],
})
export class FileRendererComponent {
  repository = input.required<ILakeFsRepository>();
  ref = input.required<string>();
  object = input.required<ILakeFsObjectStats>();

  loadProgress = signal<number>(0);
  loading = signal<boolean>(true);
  objectTextData = signal<string | null>(null);

  fileType = computed(() => {
    const obj = this.object();
    if (obj) {
      const fileType = this.guessType(getFileExtension(obj.path));
      if (fileType !== FileType.DATA && obj.sizeBytes > MAX_FILE_SIZE) {
        return FileType.TOO_LARGE;
      } else {
        return fileType;
      }
    }
    return null;
  });

  private trackProgress = effect(
    () => {
      const ft = this.fileType();
      if (ft !== FileType.TEXT && ft !== FileType.MARKDOWN && ft !== FileType.IPYNB) {
        this.loading.set(false);
        return;
      }
      const repo = this.repository()?.id;
      const ref = this.ref();
      const path = this.object()?.path;
      if (!repo || !ref || !path) return;

      this.loading.set(true);
      this.loadProgress.set(0);
      this.objectTextData.set(null);

      this.lakeFsService.fetchObjectAsTextWithProgress(repo, ref, path).subscribe({
        next: event => {
          if (event.data === null) {
            this.loadProgress.set(event.progress);
          } else {
            this.loadProgress.set(100);
            this.loading.set(false);
            this.objectTextData.set(event.data);
          }
        },
        error: () => {
          this.loading.set(false);
        },
      });
    },
    { allowSignalWrites: true },
  );

  guessType(fileExtension: string | null): FileType {
    switch (fileExtension) {
      case 'parquet':
      case 'arrow':
      case 'csv':
      case 'tsv':
        return FileType.DATA;
      case 'md':
        return FileType.MARKDOWN;
      case 'ipynb':
      case 'pynb':
        return FileType.IPYNB;
      case 'png':
      case 'jpeg':
      case 'jpg':
      case 'webm':
      case 'gif':
        return FileType.IMAGE;
      case 'pdf':
        return FileType.PDF;
      case 'txt':
      case 'text':
      case 'yaml':
      case 'yml':
      case 'xml':
      case 'html':
      case 'json':
      case 'jsonl':
      case 'py':
      case 'java':
      case 'c':
      case 'cpp':
      case 'js':
      case 'ts':
      case 'jinja':
        return FileType.TEXT;
    }

    return FileType.UNSUPPORTED;
  }

  readonly lakeFsService = inject(LakeFsService);
  readonly store = inject(Store);

  protected readonly FileType = FileType;
}

export const getFileExtension = (objectName: string): string => {
  const objectNameParts = objectName.split('.');
  return objectNameParts[objectNameParts.length - 1];
};

enum FileType {
  DATA,
  MARKDOWN,
  IPYNB,
  IMAGE,
  PDF,
  TEXT,
  UNSUPPORTED,
  TOO_LARGE,
}
