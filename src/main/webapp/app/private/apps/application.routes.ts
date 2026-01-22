import {Routes} from '@angular/router';
import {ASC} from 'app/config/constant/navigation.constants';
import {UserRouteAccessService} from '../../shared/service/user-route-access.service';
import {DeployStep1Page} from './components/create/step1/deploy-step1.page';
import {DeployStep2Page} from './components/create/step2/deploy-step2.page';
import {AppPage} from './app.page';
import { TemplateDetailsPage } from './components/create/template-details/template-details.page';

const applicationRoute: Routes = [
  {
    path: '',
    component: AppPage,
    data: {
      defaultSort: 'id,' + ASC,
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: 'deploy',
    component: DeployStep1Page,
    data: {
      breadcrumb: 'Deploy a new application'
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'deploy-step-2',
    component: DeployStep2Page,
    data: {
      breadcrumb: 'Deploy a new application'
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'template/:id',
    component: TemplateDetailsPage,
    data: {
      breadcrumb: 'Template Details'
    },
    canActivate: [UserRouteAccessService],
  },
];

export default applicationRoute;
