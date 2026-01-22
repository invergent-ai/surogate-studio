import { Routes } from '@angular/router';

import { ASC } from 'app/config/constant/navigation.constants';
import { ClusterComponent } from '../components/cluster/cluster.component';
import { UserRouteAccessService } from '../../../shared/service/user-route-access.service';

const clusterRoute: Routes = [
  {
    path: '',
    component: ClusterComponent,
    data: {
      defaultSort: 'id,' + ASC,
      breadcrumb: 'Clusters'
    },
    canActivate: [UserRouteAccessService],
  },
];

export default clusterRoute;
