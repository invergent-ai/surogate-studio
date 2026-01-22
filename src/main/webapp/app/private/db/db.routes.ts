import {Routes} from '@angular/router';
import {ASC} from 'app/config/constant/navigation.constants';
import {UserRouteAccessService} from '../../shared/service/user-route-access.service';
import {DbPage} from './db.page';
import {DeployDbPage} from "./components/create/deploy.db.page";

const dbRoute: Routes = [
  {
    path: '',
    component: DbPage,
    data: {
      defaultSort: 'id,' + ASC,
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: 'deploy',
    component: DeployDbPage,
    data: {
      breadcrumb: 'Deploy a new database'
    },
    canActivate: [UserRouteAccessService],
  }
];

export default dbRoute;
