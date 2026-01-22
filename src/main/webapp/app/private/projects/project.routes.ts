import { Routes } from '@angular/router';
import { UserRouteAccessService } from '../../shared/service/user-route-access.service';
import { ProjectPage } from './project.page';
import { CreateProjectPage } from './components/create/create-project.page';

const projectRoute: Routes = [
  {
    path: '',
    component: ProjectPage,
    data: {
      breadcrumb: 'My Projects'
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'create',
    component: CreateProjectPage,
    data: {
      breadcrumb: 'New Project'
    },
    canActivate: [UserRouteAccessService],
  },
];

export default projectRoute;
