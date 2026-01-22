import { Routes } from '@angular/router';
import { ASC } from 'app/config/constant/navigation.constants';
import { UserRouteAccessService } from '../../shared/service/user-route-access.service';
import { ModelPage } from './model.page';

const modelRoute: Routes = [
  {
    path: '',
    component: ModelPage,
    data: {
      defaultSort: 'id,' + ASC
    },
    canActivate: [UserRouteAccessService]
  }
];

export default modelRoute;
