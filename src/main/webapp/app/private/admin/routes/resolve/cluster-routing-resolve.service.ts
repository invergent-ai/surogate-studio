import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of, EMPTY, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';
import { ICluster } from '../../../../shared/model/cluster.model';
import { ClusterService } from '../../../../shared/service/cluster.service';

export const clusterResolve = (route: ActivatedRouteSnapshot): Observable<null | ICluster> => {
  const id = route.params['id'];
  if (id) {
    return inject(ClusterService)
      .find(id)
      .pipe(
        mergeMap((cluster: HttpResponse<ICluster>) => {
          if (cluster.body) {
            return of(cluster.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(null);
};

export default clusterResolve;
