import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { UserRouteAccessService } from '../../shared/service/user-route-access.service';
import { Authority } from '../../config/constant/authority.constants';
/* jhipster-needle-add-admin-module-import - JHipster will add admin modules imports here */

@NgModule({
  imports: [
    /* jhipster-needle-add-admin-module - JHipster will add admin modules here */
    RouterModule.forChild([
      {
        path: 'zone',
        data: { pageTitle: 'stateMeshApp.zone.home.title' },
        loadChildren: () => import('./routes/zone.routes'),
      },
      {
        path: 'cluster',
        data: { pageTitle: 'stateMeshApp.cluster.home.title' },
        loadChildren: () => import('./routes/cluster.routes'),
      },
      {
        path: 'system-configuration',
        data: { pageTitle: 'stateMeshApp.systemConfiguration.home.title' },
        loadChildren: () => import('./routes/system-configuration.routes'),
      },
      {
        path: 'users',
        data: { pageTitle: 'Users' },
        loadChildren: () => import('./routes/users.routes'),
      }
    ]),
  ],
})
export default class AdminRoutingModule {}
