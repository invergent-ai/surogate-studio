import { HttpEvent } from '@angular/common/http';
import { FileUploaderComponent } from './file-uploader.component';

export type UrlModifierCallback = (url: string, files: File[]) => string

/**
 * Upload event.
 * @group Events
 */
export interface UploadEvent {
  /**
   * HTTP event.
   */
  originalEvent: HttpEvent<any>;
}
/**
 * Form data event.
 * @group Events
 */
export interface FormDataEvent {
  /**
   * FormData object.
   */
  formData: FormData;
  files?: File[];
}

/**
 * An event indicating that the request was sent to the server. Useful when a request may be retried multiple times, to distinguish between retries on the final event stream.
 * @group Events
 */
export interface FileSendEvent extends UploadEvent, FormDataEvent {}
/**
 * Callback to invoke before file upload is initialized.
 * @see {@link FileUploaderComponent.onBeforeUpload}
 * @group Events
 */
export interface FileBeforeUploadEvent extends FormDataEvent {}
/**
 * Callback to invoke when file upload is complete.
 * @see {@link FileUploaderComponent.onUpload}
 * @group Events
 */
export interface FileUploadEvent extends UploadEvent {
  /**
   * Uploaded files.
   */
  files: File[];
}
/**
 * Callback to invoke when a file is removed without uploading using clear button of a file.
 * @see {@link FileUploaderComponent.onRemove}
 * @group Events
 */
export interface FileRemoveEvent {
  /**
   * Browser event.
   */
  originalEvent: Event;
  /**
   * Selected file
   */
  file: File;
}
/**
 * Callback to invoke when files are selected.
 * @see {@link FileUploaderComponent.onSelect}
 * @group Events
 */
export interface FileSelectEvent {
  /**
   * Browser event.
   */
  originalEvent: Event;
  /**
   * Uploaded files.
   */
  files: File[];
  /**
   * All files to be uploaded.
   */
  currentFiles: File[];
}
/**
 * Callback to invoke when files are being uploaded.
 * @extends {UploadEvent}
 * @group Events
 */
export interface FileProgressEvent extends UploadEvent {
  /**
   * Calculated progress value.
   */
  progress: number;
  files?: File[];
}
/**
 * Callback to invoke in custom upload mode to upload the files manually.
 * @see {@link FileUploaderComponent.uploadHandler}
 * @group Events
 */
export interface FileUploadHandlerEvent {
  /**
   * List of selected files.
   */
  files: File[];
}
/**
 * Callback to invoke on upload error.
 * @see {@link FileUploaderComponent.onError}
 * @group Events
 */
export interface FileUploadErrorEvent {
  /**
   * List of selected files.
   */
  error?: ErrorEvent;
  /**
   * List of selected files.
   */
  files: File[];
}
