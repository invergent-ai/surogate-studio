import {Component, computed, inject, input, signal} from '@angular/core';
import {ButtonDirective} from 'primeng/button';
import {CardModule} from 'primeng/card';
import {DropdownModule} from 'primeng/dropdown';
import {Bot, Download, File, LucideAngularModule, RefreshCw, Upload} from 'lucide-angular';
import {NgIf} from '@angular/common';
import {MessageService, PrimeTemplate, TreeNode} from 'primeng/api';
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {derivedAsync} from 'ngxtension/derived-async';
import {catchError, tap} from 'rxjs/operators';
import {displayError, displayErrorAndRethrow} from '../../../shared/util/error.util';
import {ILakeFsObjectStats, ILakeFsRepository} from '../../../shared/model/lakefs.model';
import {LakeFsService} from '../../../shared/service/lake-fs.service';
import {Store} from '@ngxs/store';
import {TableModule} from 'primeng/table';
import dayjs from 'dayjs/esm';
import {NgxFilesizeModule} from 'ngx-filesize';
import {TreeTableModule} from 'primeng/treetable';
import {FileActionEvent, FileNavigatorComponent} from './file-navigator.component';
import {FileRendererComponent} from './file-renderer.component';
import {DialogModule} from 'primeng/dialog';
import {FileUploadModule} from 'primeng/fileupload';
import {InputTextModule} from 'primeng/inputtext';
import {DirectLakeFsService} from '../../../shared/service/direct-lake-fs.service';
import {FileUploaderComponent} from '../../../shared/components/file-upload/file-uploader.component';
import {Clipboard} from '@angular/cdk/clipboard';
import {displaySuccess} from '../../../shared/util/success.util';
import {lastValueFrom, of} from 'rxjs';
import {saveAs} from 'file-saver';
import {RefSelection, RefSelectorComponent} from './ref-selector.component';
import {effectOnceIf} from 'ngxtension/effect-once-if';
import {MarkdownRendererComponent} from "./renderers/markdown-renderer.component";
import {repoDisplayNameOrId} from "../../../shared/util/naming.util";
import {TagModule} from "primeng/tag";

@Component({
  selector: 'sm-hub-objects',
  standalone: true,
  templateUrl: './hub-objects.component.html',
  imports: [
    ButtonDirective,
    CardModule,
    DropdownModule,
    LucideAngularModule,
    NgIf,
    PrimeTemplate,
    FormsModule,
    TableModule,
    NgxFilesizeModule,
    TreeTableModule,
    FileNavigatorComponent,
    FileRendererComponent,
    DialogModule,
    FileUploadModule,
    InputTextModule,
    FileUploaderComponent,
    ReactiveFormsModule,
    RefSelectorComponent,
    MarkdownRendererComponent,
    TagModule
  ],
  providers: [MessageService],
  styles: `
    ::ng-deep .p-card-body {
      padding: 1rem !important;
    }
  `
})
export class HubObjectsComponent {
  repository = input.required<ILakeFsRepository>();
  refId = input<string>();
  refType = input<'branch' | 'tag' | 'commit'>();

  branches = derivedAsync(() => this.lakeFsService.listBranches(this.repository().id)
    .pipe(
      tap(branches => {
        let branchToSelect = branches && branches.length > 0 ? branches[0] : null;
        if (this.refType() === 'branch' && this.refId()) {
          const existingBranch = branches.find(b => b.id === this.refId());
          if (existingBranch) {
            branchToSelect = existingBranch;
          }
        }
        if (!this.refType() || this.refType() === 'branch') {
          this.selectedRef.set({id: branchToSelect.id, type: 'branch', metadata: branchToSelect.metadata});
        }
      }),
      catchError((e) => displayErrorAndRethrow(this.store, e))));

  tags = derivedAsync(() => this.lakeFsService.listTags(this.repository().id)
    .pipe(
      tap(tags => {
        if (this.refType() === 'tag' && this.refId()) {
          const existingTag = tags.find(b => b.id === this.refId());
          if (existingTag) {
            this.selectedRef.set({id: existingTag.id, type: 'tag'});
          }
        }
      }),
      catchError((e) => displayErrorAndRethrow(this.store, e))));

  objects = derivedAsync(() => {
    this.refreshObjects();
    return this.selectedRef() ?
      this.lakeFsService.listObjects(this.repository()?.id, this.selectedRef().id)
        .pipe(catchError((e) => displayErrorAndRethrow(this.store, e))) : [] as ILakeFsObjectStats[]
  });

  refreshObjects = signal(1);
  selectedRef = signal<RefSelection>(null);
  breadcrumb = signal<TreeNode[]>([]);
  selectedFile = signal<TreeNode<ILakeFsObjectStats>>(null);
  selectedFolder = signal<TreeNode>(null);
  showBreadcrumb = computed(() => this.breadcrumb().length > 0 || this.selectedFile() !== null);

  addFileDialogVisible = signal(false);
  deleteFileDialogVisible = signal(false);

  commitForm = new FormGroup({
    path: new FormControl<string>(null, [Validators.required]),
    message: new FormControl<string>(null, [Validators.required]),
  });

  deleteForm = new FormGroup({
    message: new FormControl<string>(null, [Validators.required]),
    file: new FormControl<ILakeFsObjectStats>(null, [Validators.required]),
  });

  constructor() {
    effectOnceIf(
      () => this.refType() === 'commit',
      (valueFromCondition) => {
        if (valueFromCondition) {
          this.selectedRef.set({id: this.refId(), type: 'commit'});
        }
      }
    );

  }

  readmeText = derivedAsync(() => {
    const objs = this.objects();
    const repo = this.repository()?.id;
    const ref = this.selectedRef()?.id;

    if (!objs || !repo || !ref) {
      return of(null);
    }

    const readme = objs.find(o => o.path?.toLowerCase() === 'readme.md');
    if (!readme) {
      return of(null);
    }

    return this.directLakeFsService.fetchObjectAsText(repo, ref, readme.path).pipe(
      catchError(err => {
        console.error('❌ Eroare la încărcarea README.md', err);
        return of(null);
      })
    );
  });

  chooseRef(ref: RefSelection) {
    this.selectedRef.set(ref);
  }

  onSelectFile(file: TreeNode<ILakeFsObjectStats>) {
    this.selectedFile.set(file);
    this.updateBreadcrumbFromNode(file.parent);
  }

  onSelectFolder(folder: TreeNode) {
    this.selectedFile.set(null);
    this.selectedFolder.set(folder);
    this.updateBreadcrumbFromNode(folder);
  }

  selectRootFolder() {
    this.selectedFile.set(null);
    this.selectedFolder.set(null);
    this.breadcrumb.set([]);
  }

  selectFolderFromBreadcrumb(node: TreeNode) {
    this.selectedFile.set(null);
    this.selectedFolder.set(node);
    this.updateBreadcrumbFromNode(node);
  }

  private updateBreadcrumbFromNode(node: TreeNode | null) {
    const path: TreeNode[] = [];
    let current = node;
    while (current) {
      path.unshift(current);
      current = current.parent;
    }
    this.breadcrumb.set(path);
  }

  refresh() {
    this.refreshObjects.set(new Date().getTime());
  }

  showAddFileDialog() {
    this.addFileDialogVisible.set(true);
    this.commitForm.reset({
      path: this.selectedFolder() ? this.selectedFolder().key : '/',
      message: null
    })
  }

  uploadUrlModifier(url: string, files: File[]): string {
    return this.directLakeFsService.objectUploadUrl(
      this.repository().id,
      this.selectedRef().id,
      this.commitForm.value.path,
      files[0]);
  }

  async uploadComplete() {
    // this is needed because lakeFs might not update the list of objects instantly
    setTimeout(() => {
      this.refresh();
    }, 2000);

    try {
      await lastValueFrom(this.lakeFsService.commit(
        this.repository().id,
        this.selectedRef().id,
        {message: this.commitForm.value.message}
      ));
    } catch (e) {
      displayError(this.store, e);
    }

    this.addFileDialogVisible.set(false);
  }

  onFileAction(event: FileActionEvent) {
    if (event?.action === 'copy') {
      const url = `lakefs://${this.repository().id}/${this.selectedRef()}/${event.file.path}`;
      this.clipboard.copy(url);
      displaySuccess(this.store, 'Copied to clipboard', 1000);
    } else if (event?.action === 'delete') {
      this.deleteForm.reset({
        message: null,
        file: event.file
      });
      this.deleteFileDialogVisible.set(true);
    } else if (event?.action === 'download') {
      this.directLakeFsService.download(this.repository().id, this.selectedRef().id, event.file.path).subscribe({
        next: (response) => {
          let fileName = event.file.path;
          if (fileName.lastIndexOf('/') > -1) {
            fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
          }
          saveAs(response.body, fileName);
        }
      })
    }
  }

  async deleteFile() {
    try {
      await lastValueFrom(this.lakeFsService.deleteObject(
        this.repository().id, this.selectedRef().id, this.deleteForm.value.file.path));

      await lastValueFrom(this.lakeFsService.commit(
        this.repository().id,
        this.selectedRef().id,
        {message: this.deleteForm.value.message}
      ));

      // this is needed because lakeFs might not update the list of objects instantly
      setTimeout(() => {
        this.refresh();
      }, 2000);
    } catch (e) {
      displayError(this.store, e);
    }

    this.deleteFileDialogVisible.set(false);
  }

  getRepoName() {
    return repoDisplayNameOrId(this.repository());
  }

  readonly messageService = inject(MessageService);
  readonly directLakeFsService = inject(DirectLakeFsService);
  readonly lakeFsService = inject(LakeFsService);
  readonly store = inject(Store);
  readonly clipboard = inject(Clipboard);

  protected readonly Download = Download;
  protected readonly Upload = Upload;
  protected readonly dayjs = dayjs;
  protected readonly Object = Object;
  protected readonly File = File;
  protected readonly RefreshCw = RefreshCw;
  protected readonly Bot = Bot;
}



