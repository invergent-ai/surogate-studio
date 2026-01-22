import { Routes } from '@angular/router';

import { ASC } from 'app/config/constant/navigation.constants';
import { ZoneComponent } from '../components/zone/zone.component';
import { UserRouteAccessService } from '../../../shared/service/user-route-access.service';

const zoneRoute: Routes = [
  {
    path: '',
    component: ZoneComponent,
    data: {
      defaultSort: 'id,' + ASC,
      breadcrumb: 'Zones'
    },
    canActivate: [UserRouteAccessService],
  },
];

export default zoneRoute;
