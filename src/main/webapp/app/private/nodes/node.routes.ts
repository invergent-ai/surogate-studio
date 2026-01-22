import { Routes } from '@angular/router';
import { UserRouteAccessService } from '../../shared/service/user-route-access.service';
import { NodePage } from './node.page';

const nodeRoute: Routes = [
  {
    path: '',
    component: NodePage,
    data: {
      breadcrumb: 'My Node'
    },
    canActivate: [UserRouteAccessService],
  }
];

export default nodeRoute;
