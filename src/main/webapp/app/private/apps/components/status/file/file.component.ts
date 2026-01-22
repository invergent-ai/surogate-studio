import {ChangeDetectorRef, Component, Input, OnDestroy, OnInit} from '@angular/core';
import {MessageService} from 'primeng/api';
import {NgTerminalModule} from "ng-terminal";
import {Subject, Subscription} from "rxjs";
import {CommonModule} from "@angular/common";
import {TooltipModule} from "primeng/tooltip";
import {FileItem, FileUploader, FileUploadModule} from "ng2-file-upload";
import {Button, ButtonDirective} from "primeng/button";
import {MessageModule} from "primeng/message";
import {ScrollPanelModule} from "primeng/scrollpanel";
import {ProgressBarModule} from "primeng/progressbar";
import {Ripple} from "primeng/ripple";
import {finalize, takeUntil} from "rxjs/operators";
import {InputTextModule} from "primeng/inputtext";
import {PaginatorModule} from "primeng/paginator";
import {ReactiveFormsModule} from "@angular/forms";
import {ProgressSpinnerModule} from "primeng/progressspinner";
import {FileService} from "../../../../../shared/service/k8s/file.service";

@Component({
  selector: 'sm-file',
  standalone: true,
  imports: [
    CommonModule,
    NgTerminalModule,
    TooltipModule,
    FileUploadModule,
    Button,
    MessageModule,
    FileUploadModule,
    ScrollPanelModule,
    FileUploadModule,
    ProgressBarModule,
    ButtonDirective,
    Ripple,
    InputTextModule,
    PaginatorModule,
    ReactiveFormsModule,
    ProgressSpinnerModule
  ],
  templateUrl: './file.component.html',
  styleUrls: ['./file.component.scss'],
})
export class FileComponent implements OnInit, OnDestroy {
  @Input() applicationId!: string;
  @Input() podName!: string;
  @Input() containerId?: string;

  public readonly supportedExtensions =
    ['.pdf', '.jpg', '.jpeg', '.png', '.tif', '.tiff', '.odt', '.txt', '.doc', '.docx', '.yml', '.yaml', '.properties'];
  public readonly maxFileSizeInBytes = 20 * 1024 * 1024; // 20MB file size limit

  uploader: FileUploader;
  uploadProgress = 0;
  uploadError = '';
  dragOver = false;
  isUploading = false;
  path: string;
  downloadPath: string;

  private destroy$ = new Subject<void>();
  private subscriptions = new Subscription();

  constructor(private messageService: MessageService,
              private fileService: FileService,
              private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.uploader = new FileUploader({
      url: '', // We'll handle upload manually using our upload service
      autoUpload: false,
      maxFileSize: this.maxFileSizeInBytes,
      removeAfterUpload: true,
      itemAlias: 'file',
    });
  }

  onFileOver(event: boolean): void {
    this.dragOver = event;
    this.cdr.markForCheck();
  }

  onFileDrop(files: File[]): void {
    this.dragOver = false;
    this.uploadError = '';
    if (files) {
      const supportedFilesToAdd: File[] = [];
      const unsupportedFiles: string[] = [];
      const oversizedFiles: string[] = [];

      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        // Check file type and size
        const isTypeSupported = this.isFileTypeSupported(file.name);
        const isSizeValid = this.isFileSizeValid(file.size);
        if (!isTypeSupported) {
          unsupportedFiles.push(file.name);
        } else if (!isSizeValid) {
          oversizedFiles.push(file.name);
        } else {
          supportedFilesToAdd.push(file);
        }
      }

      // Show error messages if needed
      if (unsupportedFiles.length > 0) {
        const errorMsg = `Unsupported file type(s): ${unsupportedFiles.join(', ')}. Only specific document and image types are supported.`;
        this.uploadError = errorMsg;
        this.messageService.add({
          severity: 'error',
          summary: 'Unsupported File Type',
          detail: errorMsg,
          life: 5000
        });
      }
      if (oversizedFiles.length > 0) {
        const fileSizeMB = Math.round(this.maxFileSizeInBytes / (1024 * 1024));
        const errorMsg = `File(s) exceed the maximum allowed size of ${fileSizeMB} MB: ${oversizedFiles.join(', ')}`;
        this.uploadError = this.uploadError ? `${this.uploadError}\n${errorMsg}` : errorMsg;
        this.messageService.add({
          severity: 'error',
          summary: 'Files Too Large',
          detail: errorMsg,
          life: 5000
        });
      }
      if (supportedFilesToAdd.length > 0) {
        this.uploader.addToQueue(supportedFilesToAdd);
      }
      this.cdr.markForCheck();
    }
  }

  removeFile(item: FileItem, event: Event): void {
    event.stopPropagation();
    this.uploader.removeFromQueue(item);
    if (this.uploader.queue.length === 0) {
      this.uploadError = '';
    }
    this.cdr.markForCheck();
  }

  clearAll(event: Event): void {
    event.stopPropagation();
    this.uploader.clearQueue();
    this.cdr.markForCheck();
  }

  isFileTypeSupported(fileName: string | undefined): boolean {
    if (!fileName) {
      return false;
    }
    const extension = '.' + fileName.split('.').pop()?.toLowerCase();
    return !!extension && this.supportedExtensions.includes(extension);
  }

  isFileSizeValid(fileSize: number | undefined): boolean {
    if (!fileSize) {
      return false;
    }
    return fileSize <= this.maxFileSizeInBytes;
  }

  downloadFile(event: Event) {
    event.stopPropagation();
    if (!this.downloadPath) {
      this.messageService.add({
        severity: 'error',
        summary: 'Path is missing',
        detail: 'Please insert the absolute path in container of the file you want to download'
      });
      return;
    }
    window.open(this.fileService.downloadUrl(this.applicationId, this.podName, this.downloadPath, this.containerId), '_blank');
  }

  uploadFiles(event: Event): void {
    event.stopPropagation();
    if (!this.path) {
      this.messageService.add({
        severity: 'error',
        summary: 'Path is missing',
        detail: 'Please insert the path in container where you want to upload the files'
      });
      return;
    }
    if (!this.uploader.queue.length || this.uploadError) {
      return;
    }

    this.uploadError = '';
    this.uploadProgress = 0;
    this.isUploading = true;
    // Use a shared state object to track upload progress
    const uploadState = {
      totalFiles: this.uploader.queue.length,
      filesUploaded: 0,
      filesWithErrors: 0
    };
    // Process each file in the queue
    this.uploader.queue.forEach((item) => {
      const file = item._file;
      if (!file) return;

      this.uploadFile(file, uploadState, item);
    });

    this.cdr.markForCheck();
  }

  private uploadFile(
    file: File,
    uploadState: { totalFiles: number, filesUploaded: number, filesWithErrors: number },
    fileItem: FileItem
  ): void {
    // Update file item to show it's processing
    fileItem.isUploading = true;

    // Create subscription to track and manage the request
    const uploadSubscription = this.fileService.uploadFileToContainer({
      applicationId: this.applicationId,
      podName: this.podName,
      containerId: this.containerId,
      path: this.path,
      file: file
    }).pipe(
        takeUntil(this.destroy$),
      ).pipe(
        finalize(() => {
          fileItem.isUploading = false;
          if (uploadState.filesUploaded + uploadState.filesWithErrors === uploadState.totalFiles) {
            this.isUploading = false;
            this.cdr.markForCheck();
          }
        })
      )
      .subscribe({
        next: (_: any) => {
          uploadState.filesUploaded++;
          fileItem.isSuccess = true;
          fileItem.progress = 100;
          this.uploadProgress = Math.floor((uploadState.filesUploaded / uploadState.totalFiles) * 100);
          this.cdr.markForCheck();
          this.handleUploadCompletion(uploadState);
        },
        error: (error: any) => {
          uploadState.filesWithErrors++;
          fileItem.isError = true;
          fileItem.isSuccess = false;
          this.handleUploadError(error, file);
        }
      });

    // Add to subscription management
    this.subscriptions.add(uploadSubscription);
  }

  private handleUploadCompletion(uploadState: { totalFiles: number, filesUploaded: number, filesWithErrors: number }): void {
    if (uploadState.filesUploaded + uploadState.filesWithErrors === uploadState.totalFiles) {
      if (uploadState.filesWithErrors === 0) {
        this.messageService.add({
          severity: 'success',
          summary: 'Upload Complete',
          detail: `Successfully uploaded ${uploadState.filesUploaded} file(s)`
        });
      } else {
        this.messageService.add({
          severity: 'warn',
          summary: 'Upload Partially Complete',
          detail: `Uploaded ${uploadState.filesUploaded} file(s), ${uploadState.filesWithErrors} file(s) failed`
        });
      }
    }
  }

  private handleUploadError(error: any, file: File): void {
    this.uploadError = `Failed to upload ${file.name}: ${error.error?.error?.message || error.message || 'Unknown error'}`;
    this.messageService.add({
      severity: 'error',
      summary: 'Upload Failed',
      detail: this.uploadError,
      life: 5000
    });
    this.cdr.markForCheck();
  }

  async ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    this.subscriptions.unsubscribe();
  }
}
