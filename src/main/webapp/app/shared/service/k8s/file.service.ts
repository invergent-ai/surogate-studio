import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';

import {Store} from '@ngxs/store';
import {Selectors} from "../../state/selectors";

@Injectable({providedIn: 'root'})
export class FileService {
  protected resourceUrl: string;

  constructor(
    protected http: HttpClient,
    protected store: Store,
  ) {
    store.select(Selectors.getEndpointFor('/api/files')).subscribe((url) => {
      this.resourceUrl = url;
    });
  }

  uploadFileToContainer(params: {
    applicationId: string;
    podName: string;
    containerId?: string;
    path: string;
    file?: File;
  }): Observable<any> {
    const formData = new FormData();

    formData.append('applicationId', params.applicationId);
    formData.append('podName', params.podName);
    if (params.containerId) {
      formData.append('containerId', params.containerId);
    }
    formData.append('path', params.path);
    formData.append('file', params.file);

    return this.http.post<any>(`${this.resourceUrl}/upload`, formData);
  }

  downloadUrl(applicationId: string, podName: string, path: string, containerId?: string) {
    return `${this.resourceUrl}/download?applicationId=${applicationId}&podName=${podName}${containerId ? '&containerId=' + containerId : ''}&path=${path}`;
  }
}
