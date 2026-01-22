import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of, EMPTY, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IZone } from '../../../../shared/model/zone.model';
import { ZoneService } from '../../../../shared/service/zone.service';

export const zoneResolve = (route: ActivatedRouteSnapshot): Observable<null | IZone> => {
  const id = route.params['id'];
  if (id) {
    return inject(ZoneService)
      .find(id)
      .pipe(
        mergeMap((zone: HttpResponse<IZone>) => {
          if (zone.body) {
            return of(zone.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(null);
};

export default zoneResolve;
