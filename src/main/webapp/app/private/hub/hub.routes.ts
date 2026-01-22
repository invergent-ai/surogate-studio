import { Routes } from '@angular/router';
import { UserRouteAccessService } from '../../shared/service/user-route-access.service';
import { HubPage } from './hub.page';
import { RepoPage } from './repo.page';
import { BranchesPage } from './branches-page';
import { TagsPage } from './tags.page';
import { CommitsPage } from './commits.page';
import {SettingsPage} from "./settings-page";

const hubRoute: Routes = [
  {
    path: '',
    component: HubPage,
    data: {
      breadcrumb: 'Surogate Hub'
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'files/:repo',
    component: RepoPage,
    data: {
      breadcrumb: 'Repository'
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'branches/:repo',
    component: BranchesPage,
    data: {
      breadcrumb: 'Repository'
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'tags/:repo',
    component: TagsPage,
    data: {
      breadcrumb: 'Repository'
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'commits/:repo',
    component: CommitsPage,
    data: {
      breadcrumb: 'Repository'
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'settings/:repo',
    component: SettingsPage,
    data: {
      breadcrumb: 'Repository'
    },
    canActivate: [UserRouteAccessService],
  }
];

export default hubRoute;
