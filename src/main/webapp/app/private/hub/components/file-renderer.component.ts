import { Component, computed, inject, input } from '@angular/core';
import { ILakeFsObjectStats, ILakeFsRepository } from '../../../shared/model/lakefs.model';
import { DuckDBRendererComponent } from './renderers/duckdb-renderer.component';
import { PdfRendererComponent } from './renderers/pdf-renderer.component';
import { UnsupportedRendererComponent } from './renderers/unsupported-renderer.component';
import { MarkdownRendererComponent } from './renderers/markdown-renderer.component';
import { IpynbRendererComponent } from './renderers/ipynb-renderer.component';
import { ImageRendererComponent } from './renderers/image-renderer.component';
import { ToolargeRendererComponent } from './renderers/toolarge-renderer.component';
import { derivedAsync } from 'ngxtension/derived-async';
import { catchError } from 'rxjs/operators';
import { displayErrorAndRethrow } from '../../../shared/util/error.util';
import { Store } from '@ngxs/store';
import { DirectLakeFsService } from '../../../shared/service/direct-lake-fs.service';
import { LazyTextViewerComponent } from './lazy-text-viewer';

const MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB

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
  ],
})
export class FileRendererComponent {
  repository = input.required<ILakeFsRepository>();
  ref = input.required<string>();
  object = input.required<ILakeFsObjectStats>();

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

  objectTextData = derivedAsync(() =>
    this.directLakeFsService
      .fetchObjectAsText(this.repository().id, this.ref(), this.object().path, false)
      .pipe(catchError(e => displayErrorAndRethrow(this.store, e))),
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

  readonly directLakeFsService = inject(DirectLakeFsService);
  readonly store = inject(Store);

  protected readonly FileType = FileType;
}

export const getFileExtension = (objectName: string): string => {
  const objectNameParts = objectName.split(".");
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
