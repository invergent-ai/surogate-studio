import SettingsComponent from './settings.component';
import { Routes } from '@angular/router';
import { UserRouteAccessService } from '../../shared/service/user-route-access.service';

const settingsRoutes: Routes = [{
  path: 'settings',
  component: SettingsComponent,
  title: 'global.menu.account.settings',
  data: { breadcrumb: 'Profile' },
  canActivate: [UserRouteAccessService]
}];

export default settingsRoutes;
