import {Routes} from '@angular/router';

import {ASC} from 'app/config/constant/navigation.constants';
import {SystemConfigurationComponent} from '../components/system-configuration/system-configuration.component';
import { UserRouteAccessService } from '../../../shared/service/user-route-access.service';

const systemConfigurationRoute: Routes = [
  {
    path: '',
    component: SystemConfigurationComponent,
    data: {
      defaultSort: 'id,' + ASC,
      breadcrumb: 'System configuration'
    },
    canActivate: [UserRouteAccessService],
  },
];

export default systemConfigurationRoute;
