import { Routes } from '@angular/router';
import { VolumePage } from './volume.page';
import { UserRouteAccessService } from '../../shared/service/user-route-access.service';

const volumeRoutes: Routes = [
  {
    path: '',
    component: VolumePage,
    data: {
      breadcrumb: 'Volume'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: 'create',
    component: VolumePage,
    data: {
      breadcrumb: 'Create volume'
    },
    canActivate: [UserRouteAccessService]
  }
];

export default volumeRoutes;
