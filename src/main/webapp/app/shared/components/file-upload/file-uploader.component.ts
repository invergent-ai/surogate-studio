import { DOCUMENT, isPlatformBrowser, NgClass, NgIf, NgTemplateOutlet } from '@angular/common';
import { HttpClient, HttpEvent, HttpEventType, HttpHeaders } from '@angular/common/http';
import {
  AfterContentInit,
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ContentChildren,
  ElementRef,
  EventEmitter,
  Inject,
  Input,
  NgZone,
  OnDestroy,
  OnInit,
  Output,
  PLATFORM_ID,
  QueryList,
  Renderer2,
  TemplateRef,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { BlockableUI, Message, PrimeNGConfig, PrimeTemplate, TranslationKeys } from 'primeng/api';
import { DomHandler } from 'primeng/dom';
import { VoidListener } from 'primeng/ts-helpers';
import { Subscription } from 'rxjs';
import {
  FileBeforeUploadEvent,
  FileProgressEvent,
  FileRemoveEvent,
  FileSelectEvent,
  FileSendEvent,
  FileUploadErrorEvent,
  FileUploadEvent,
  UrlModifierCallback
} from './fileupload.interface';
import { Button, ButtonDirective } from 'primeng/button';
import { ProgressBarModule } from 'primeng/progressbar';
import { MessagesModule } from 'primeng/messages';
import { CloudUpload, LucideAngularModule, Plus, X } from 'lucide-angular';
import pMap from 'p-map';
import { NgxFilesizeModule } from 'ngx-filesize';


@Component({
  selector: 'sm-file-uploader',
  standalone: true,
  templateUrl: './file-uploader.component.html',
  styleUrls: ['./file-uploader.component.scss'],
  imports: [
    NgClass,
    Button,
    ProgressBarModule,
    MessagesModule,
    LucideAngularModule,
    ButtonDirective,
    NgTemplateOutlet,
    NgIf,
    NgxFilesizeModule,
  ],
  host: {
    class: 'p-element',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
})
export class FileUploaderComponent implements AfterViewInit, AfterContentInit, OnInit, OnDestroy, BlockableUI {
  /**
   * If the uploader should upload all files in parallel.
   * In this case, the name input must not be an array
   * @group Props
   */
  @Input() parallel!: boolean;
  /**
   * Name of the request parameter to identify the files at backend.
   * @group Props
   */
  @Input() name: string | undefined;
  /**
   * Remote url to upload the files.
   * @group Props
   */
  @Input() url: string | undefined;
  /**
   * HTTP method to send the files to the url such as "post" and "put".
   * @group Props
   */
  @Input() method: 'post' | 'put' | undefined = 'post';
  /**
   * Used to select multiple files at once from file dialog.
   * @group Props
   */
  @Input() multiple: boolean | undefined;
  /**
   * Comma-separated list of pattern to restrict the allowed file types. Can be any combination of either the MIME types (such as "image/*") or the file extensions (such as ".jpg").
   * @group Props
   */
  @Input() accept: string | undefined;
  /**
   * Disables the upload functionality.
   * @group Props
   */
  @Input() disabled: boolean | undefined;
  /**
   * Cross-site Access-Control requests should be made using credentials such as cookies, authorization headers or TLS client certificates.
   * @group Props
   */
  @Input() withCredentials: boolean | undefined;
  /**
   * Maximum file size allowed in bytes.
   * @group Props
   */
  @Input() maxFileSize: number | undefined;
  /**
   * Summary message of the invalid file size.
   * @group Props
   */
  @Input() invalidFileSizeMessageSummary: string = '{0}: Invalid file size, ';
  /**
   * Detail message of the invalid file size.
   * @group Props
   */
  @Input() invalidFileSizeMessageDetail: string = 'maximum upload size is {0}.';
  /**
   * Summary message of the invalid file type.
   * @group Props
   */
  @Input() invalidFileTypeMessageSummary: string = '{0}: Invalid file type, ';
  /**
   * Detail message of the invalid file type.
   * @group Props
   */
  @Input() invalidFileTypeMessageDetail: string = 'allowed file types: {0}.';
  /**
   * Detail message of the invalid file type.
   * @group Props
   */
  @Input() invalidFileLimitMessageDetail: string = 'limit is {0} at most.';
  /**
   * Summary message of the invalid file type.
   * @group Props
   */
  @Input() invalidFileLimitMessageSummary: string = 'Maximum number of files exceeded, ';
  /**
   * Inline style of the element.
   * @group Props
   */
  @Input() style: { [klass: string]: any } | null | undefined;
  /**
   * Class of the element.
   * @group Props
   */
  @Input() styleClass: string | undefined;
  /**
   * Width of the image thumbnail in pixels.
   * @group Props
   */
  @Input() previewWidth: number = 50;
  /**
   * Whether to show the upload button.
   * @group Props
   */
  @Input() showUploadButton: boolean = true;
  /**
   * Whether to show the cancel button.
   * @group Props
   */
  @Input() showCancelButton: boolean = true;
  /**
   * Defines the UI of the component.
   * @group Props
   */
  @Input() mode: 'advanced' | 'basic' | undefined = 'advanced';
  /**
   * HttpHeaders class represents the header configuration options for an HTTP request.
   * @group Props
   */
  @Input() headers: HttpHeaders | undefined;
  /**
   * Maximum number of files that can be uploaded.
   * @group Props
   */
  @Input() fileLimit: number | undefined;
  /**
   * Callback to invoke before file upload is initialized.
   * @param {FileBeforeUploadEvent} event - Custom upload event.
   * @group Emits
   */
  @Output() onBeforeUpload: EventEmitter<FileBeforeUploadEvent> = new EventEmitter<FileBeforeUploadEvent>();
  /**
   * An event indicating that the request was sent to the server. Useful when a request may be retried multiple times, to distinguish between retries on the final event stream.
   * @param {FileSendEvent} event - Custom send event.
   * @group Emits
   */
  @Output() onSend: EventEmitter<FileSendEvent> = new EventEmitter<FileSendEvent>();
  /**
   * Callback to invoke when file upload is complete.
   * @param {FileUploadEvent} event - Custom upload event.
   * @group Emits
   */
  @Output() onUpload: EventEmitter<FileUploadEvent> = new EventEmitter<FileUploadEvent>();
  /**
   * Callback to invoke when all uploads are complete.
   * @group Emits
   */
  @Output() onUploadComplete: EventEmitter<any> = new EventEmitter<any>();
  /**
   * Callback to invoke if file upload fails.
   * @param {FileUploadErrorEvent} event - Custom error event.
   * @group Emits
   */
  @Output() onError: EventEmitter<FileUploadErrorEvent> = new EventEmitter<FileUploadErrorEvent>();
  /**
   * Callback to invoke when files in queue are removed without uploading using clear all button.
   * @param {Event} event - Browser event.
   * @group Emits
   */
  @Output() onClear: EventEmitter<Event> = new EventEmitter<Event>();
  /**
   * Callback to invoke when a file is removed without uploading using clear button of a file.
   * @param {FileRemoveEvent} event - Remove event.
   * @group Emits
   */
  @Output() onRemove: EventEmitter<FileRemoveEvent> = new EventEmitter<FileRemoveEvent>();
  /**
   * Callback to invoke when files are selected.
   * @param {FileSelectEvent} event - Select event.
   * @group Emits
   */
  @Output() onSelect: EventEmitter<FileSelectEvent> = new EventEmitter<FileSelectEvent>();
  /**
   * Callback to invoke when files are being uploaded.
   * @param {FileProgressEvent} event - Progress event.
   * @group Emits
   */
  @Output() onProgress: EventEmitter<FileProgressEvent> = new EventEmitter<FileProgressEvent>();

  @ContentChildren(PrimeTemplate) templates: QueryList<PrimeTemplate> | undefined;

  @ViewChild('advancedfileinput') advancedFileInput: ElementRef | undefined | any;

  @ViewChild('content') content: ElementRef | undefined;

  @Input() set files(files) {
    this._files = [];

    for (let i = 0; i < files.length; i++) {
      let file = files[i];

      if (this.validate(file)) {
        this._files.push(files[i]);
      }
    }
  }

  get files(): File[] {
    return this._files;
  }

  @Input() urlModifier: UrlModifierCallback = undefined;

  public _files: File[] = [];
  public progress: number = 0;
  public fileProgress: number[] = [];
  public dragHighlight: boolean | undefined;
  public msgs: Message[] | undefined;
  public contentTemplate: TemplateRef<any> | undefined;
  public toolbarTemplate: TemplateRef<any> | undefined;
  public uploadedFileCount: number = 0;
  focus: boolean | undefined;
  uploading: boolean | undefined;
  duplicateIEEvent: boolean | undefined; // flag to recognize duplicate onchange event for file input
  translationSubscription: Subscription | undefined;
  dragOverListener: VoidListener;

  constructor(
    @Inject(DOCUMENT) private document: Document,
    @Inject(PLATFORM_ID) private platformId: any,
    private renderer: Renderer2,
    private el: ElementRef,
    public zone: NgZone,
    private http: HttpClient,
    public cd: ChangeDetectorRef,
    public config: PrimeNGConfig,
  ) {}

  ngAfterContentInit() {
    this.templates?.forEach(item => {
      switch (item.getType()) {
        case 'content':
          this.contentTemplate = item.template;
          break;
        case 'toolbar':
          this.toolbarTemplate = item.template;
          break;
      }
    });
  }

  ngOnInit() {
    this.translationSubscription = this.config.translationObserver.subscribe(() => {
      this.cd.markForCheck();
    });
  }

  ngAfterViewInit() {
    if (isPlatformBrowser(this.platformId)) {
      if (this.mode === 'advanced') {
        this.zone.runOutsideAngular(() => {
          if (this.content) {
            this.dragOverListener = this.renderer.listen(this.content.nativeElement, 'dragover', this.onDragOver.bind(this));
          }
        });
      }
    }
  }

  getTranslation(option: string) {
    return this.config.getTranslation(option);
  }

  choose() {
    this.advancedFileInput?.nativeElement.click();
  }

  onFileSelect(event: any) {
    if (event.type !== 'drop' && this.isIE11() && this.duplicateIEEvent) {
      this.duplicateIEEvent = false;
      return;
    }

    this.msgs = [];
    if (!this.multiple) {
      this.files = [];
    }

    let files = event.dataTransfer ? event.dataTransfer.files : event.target.files;
    for (let i = 0; i < files.length; i++) {
      let file = files[i];

      if (!this.isFileSelected(file)) {
        if (this.validate(file)) {
          this.files.push(files[i]);
        }
      }
    }

    this.onSelect.emit({ originalEvent: event, files: files, currentFiles: this.files });

    if (this.fileLimit) {
      this.checkFileLimit();
    }

    if (event.type !== 'drop' && this.isIE11()) {
      this.clearIEInput();
    } else {
      this.clearInputElement();
    }
  }

  isFileSelected(file: File): boolean {
    for (let sFile of this.files) {
      if (sFile.name + sFile.type + sFile.size === file.name + file.type + file.size) {
        return true;
      }
    }

    return false;
  }

  isIE11() {
    if (isPlatformBrowser(this.platformId)) {
      return !!(this.document.defaultView as any)['MSInputMethodContext'] && !!(this.document as any)['documentMode'];
    }
    return false;
  }

  validate(file: File): boolean {
    this.msgs = this.msgs || [];
    if (this.accept && !this.isFileTypeValid(file)) {
      this.msgs.push({
        severity: 'error',
        summary: this.invalidFileTypeMessageSummary.replace('{0}', file.name),
        detail: this.invalidFileTypeMessageDetail.replace('{0}', this.accept),
      });
      return false;
    }

    if (this.maxFileSize && file.size > this.maxFileSize) {
      this.msgs.push({
        severity: 'error',
        summary: this.invalidFileSizeMessageSummary.replace('{0}', file.name),
        detail: this.invalidFileSizeMessageDetail.replace('{0}', this.formatSize(this.maxFileSize)),
      });
      return false;
    }

    return true;
  }

  private isFileTypeValid(file: File): boolean {
    let acceptableTypes = this.accept?.split(',').map(type => type.trim());
    for (let type of acceptableTypes!) {
      let acceptable = this.isWildcard(type)
        ? this.getTypeClass(file.type) === this.getTypeClass(type)
        : file.type == type || this.getFileExtension(file).toLowerCase() === type.toLowerCase();

      if (acceptable) {
        return true;
      }
    }

    return false;
  }

  getTypeClass(fileType: string): string {
    return fileType.substring(0, fileType.indexOf('/'));
  }

  isWildcard(fileType: string): boolean {
    return fileType.indexOf('*') !== -1;
  }

  getFileExtension(file: File): string {
    return '.' + file.name.split('.').pop();
  }

  /**
   * Uploads the selected files.
   * @group Method
   */
  async upload() {
    if (this.parallel) {
      await this.uploadParallel();
    } else {
      this.uploadStandard();
    }
  }

  async uploadParallel() {
    this.uploading = true;
    this.msgs = [];
    this.fileProgress = [];
    this.files.forEach(() => this.fileProgress.push(0));

    const abortController = new AbortController();
    await pMap(
      this.files,
      (file, index) => {
        let formData = new FormData();
        formData.append(this.name!, this.files[index], this.files[index].name);
        this.onBeforeUpload.emit({ formData });

        let url = this.url;
        if (this.urlModifier) {
          url = this.urlModifier(this.url, [this.files[index]]);
        }

        return new Promise<void>((resolve, reject) => {
          this.http
            .request(<string>this.method, url as string, {
              body: formData,
              headers: this.headers,
              reportProgress: true,
              observe: 'events',
              withCredentials: this.withCredentials,
            })
            .subscribe({
              next: (event: HttpEvent<any>) => {
                switch (event.type) {
                  case HttpEventType.Sent:
                    this.onSend.emit({ originalEvent: event, formData, files: [this.files[index]] });
                    this.cd.markForCheck();
                    break;
                  case HttpEventType.Response:
                    this.uploading = false;
                    this.fileProgress[index] = 0;
                    if (event['status'] >= 200 && event['status'] < 300) {
                      if (this.fileLimit) {
                        this.uploadedFileCount += this.files.length;
                      }
                      this.onUpload.emit({ originalEvent: event, files: [this.files[index]] });
                    } else {
                      this.onError.emit({ files: [this.files[index]] });
                    }
                    this.clear();
                    resolve();
                    break;
                  case HttpEventType.UploadProgress:
                    if (event['loaded']) {
                      this.fileProgress[index] = Math.round((event['loaded'] * 100) / event['total']!);
                    }
                    this.onProgress.emit({ originalEvent: event, progress: this.fileProgress[index], files: [this.files[index]] });
                    this.cd.markForCheck();
                    break;
                }
              },
              error: (error: ErrorEvent) => {
                this.uploading = false;
                this.onError.emit({ files: [this.files[index]], error });
                reject(error);
              },
            });
        });
      },
      { concurrency: 5, signal: abortController.signal },
    );

    this.onUploadComplete.emit();
  }

  uploadStandard() {
    this.uploading = true;
    this.msgs = [];
    let formData = new FormData();
    for (let i = 0; i < this.files.length; i++) {
      formData.append(this.name!, this.files[i], this.files[i].name);
    }
    this.onBeforeUpload.emit({
      formData: formData,
    });

    let url = this.url;
    if (this.urlModifier) {
      url = this.urlModifier(this.url, this.files);
    }

    this.http
      .request(<string>this.method, url as string, {
        body: formData,
        headers: this.headers,
        reportProgress: true,
        observe: 'events',
        withCredentials: this.withCredentials,
      })
      .subscribe({
        next: (event: HttpEvent<any>) => {
          switch (event.type) {
            case HttpEventType.Sent:
              this.onSend.emit({
                originalEvent: event,
                formData: formData,
                files: this.files,
              });
              break;
            case HttpEventType.Response:
              this.uploading = false;
              this.progress = 0;

              if (event['status'] >= 200 && event['status'] < 300) {
                if (this.fileLimit) {
                  this.uploadedFileCount += this.files.length;
                }

                this.onUpload.emit({ originalEvent: event, files: this.files });
                this.onUploadComplete.emit();
              } else {
                this.onError.emit({ files: this.files });
              }

              this.clear();
              break;
            case HttpEventType.UploadProgress: {
              if (event['loaded']) {
                this.progress = Math.round((event['loaded'] * 100) / event['total']!);
              }

              this.onProgress.emit({ originalEvent: event, progress: this.progress, files: this.files });
              break;
            }
          }

          this.cd.markForCheck();
        },
        error: (error: ErrorEvent) => {
          this.uploading = false;
          this.onError.emit({ files: this.files, error: error });
        },
      });
  }

  /**
   * Clears the files list.
   * @group Method
   */
  clear() {
    this.files = [];
    this.uploadedFileCount = 0;
    this.onClear.emit();
    this.clearInputElement();
    this.cd.markForCheck();
  }

  remove(event: Event, index: number) {
    this.clearInputElement();
    this.onRemove.emit({ originalEvent: event, file: this.files[index] });
    this.files.splice(index, 1);
    this.checkFileLimit();
  }

  isFileLimitExceeded() {
    const totalFileCount = this.files.length + this.uploadedFileCount;
    if (this.fileLimit && this.fileLimit <= totalFileCount && this.focus) {
      this.focus = false;
    }

    return this.fileLimit && this.fileLimit < totalFileCount;
  }

  isChooseDisabled() {
    return this.fileLimit && this.fileLimit <= this.files.length + this.uploadedFileCount;
  }

  checkFileLimit() {
    this.msgs ??= [];
    if (this.isFileLimitExceeded()) {
      this.msgs.push({
        severity: 'error',
        summary: this.invalidFileLimitMessageSummary.replace('{0}', (this.fileLimit as number).toString()),
        detail: this.invalidFileLimitMessageDetail.replace('{0}', (this.fileLimit as number).toString()),
      });
    }
  }

  clearInputElement() {
    if (this.advancedFileInput && this.advancedFileInput.nativeElement) {
      this.advancedFileInput.nativeElement.value = '';
    }
  }

  clearIEInput() {
    if (this.advancedFileInput && this.advancedFileInput.nativeElement) {
      this.duplicateIEEvent = true; //IE11 fix to prevent onFileChange trigger again
      this.advancedFileInput.nativeElement.value = '';
    }
  }

  hasFiles(): boolean {
    return this.files && this.files.length > 0;
  }

  onDragEnter(e: DragEvent) {
    if (!this.disabled) {
      e.stopPropagation();
      e.preventDefault();
    }
  }

  onDragOver(e: DragEvent) {
    if (!this.disabled) {
      DomHandler.addClass(this.content?.nativeElement, 'p-fileupload-highlight');
      this.dragHighlight = true;
      e.stopPropagation();
      e.preventDefault();
    }
  }

  onDragLeave(event: DragEvent) {
    if (!this.disabled) {
      DomHandler.removeClass(this.content?.nativeElement, 'p-fileupload-highlight');
    }
  }

  onDrop(event: any) {
    if (!this.disabled) {
      DomHandler.removeClass(this.content?.nativeElement, 'p-fileupload-highlight');
      event.stopPropagation();
      event.preventDefault();

      let files = event.dataTransfer ? event.dataTransfer.files : event.target.files;
      let allowDrop = this.multiple || (files && files.length === 1);

      if (allowDrop) {
        this.onFileSelect(event);
      }
    }
  }

  onFocus() {
    this.focus = true;
  }

  onBlur() {
    this.focus = false;
  }

  formatSize(bytes: number) {
    const k = 1024;
    const dm = 3;
    const sizes = this.getTranslation(TranslationKeys.FILE_SIZE_TYPES);

    if (bytes === 0) {
      return `0 ${sizes[0]}`;
    }

    const i = Math.floor(Math.log(bytes) / Math.log(k));
    const formattedSize = (bytes / Math.pow(k, i)).toFixed(dm);

    return `${formattedSize} ${sizes[i]}`;
  }

  getBlockableElement(): HTMLElement {
    return this.el.nativeElement.children[0];
  }

  ngOnDestroy() {
    if (this.content && this.content.nativeElement) {
      if (this.dragOverListener) {
        this.dragOverListener();
        this.dragOverListener = null;
      }
    }

    if (this.translationSubscription) {
      this.translationSubscription.unsubscribe();
    }
  }

  protected readonly Plus = Plus;
  protected readonly CloudUpload = CloudUpload;
  protected readonly X = X;
}
