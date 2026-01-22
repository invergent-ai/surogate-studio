import {Routes} from "@angular/router";
import {PrivateLayoutComponent} from './layout/components/layout/private-layout.component';
import {UserRouteAccessService} from '../shared/service/user-route-access.service';
import {LogoutComponent} from './logout/logout.component';
import {Authority} from '../config/constant/authority.constants';
import SettingsComponent from './profile/settings.component';
import {DeployComponent} from "./deploy/deploy.component";

export const privateRoutes: Routes = [
  {
    path: '',
    component: PrivateLayoutComponent,
    canActivate: [UserRouteAccessService],
    children: [
      {
        path: '',
        loadChildren: () => import('./home/home.route')
      },
      {
        path: 'deploy',
        component: DeployComponent,
      },
      {
        path: 'projects',
        loadChildren: () => import('./projects/project.routes')
      },
      {
        path: 'apps',
        loadChildren: () => import('./apps/application.routes'),
      },
      {
        path: 'models',
        loadChildren: () => import('./apps/model.routes'),
      },
      {
        path: 'train',
        loadChildren: () => import('./training/training.routes'),
      },
      {
        path: 'data',
        loadChildren: () => import('./datasets/datasets.routes'),
      },
      {
        path: 'dbs',
        loadChildren: () => import('./db/db.routes'),
      },
      {
        path: 'volumes',
        loadChildren: () => import('./volumes/volume.routes'),
      },
      {
        path: 'nodes',
        loadChildren: () => import('./nodes/node.routes')
      },
      {
        path: 'logout',
        component: LogoutComponent
      },
      {
        path: 'profile',
        loadChildren: () => import('./profile/settings.route')
      },
      {
        path: 'hub',
        loadChildren: () => import('./hub/hub.routes')
      },
      {
        path: 'admin',
        data: {
          authorities: [Authority.ADMIN],
          breadcrumb: 'Admin'
        },
        canActivate: [UserRouteAccessService],
        loadChildren: () => import('./admin/admin-routing.module'),
      },
      {
        path: 'settings',
        component: SettingsComponent,
        title: 'global.menu.account.settings',
        data: { breadcrumb: 'Profile' },
        canActivate: [UserRouteAccessService],
      }
    ]
  }
];
