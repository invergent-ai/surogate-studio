import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { environment } from 'environments/environment';
import publicRoute from './public/public.route';
import ErrorComponent from './error/error/error.component';
import { AccessdeniedComponent } from './error/accessdenied/accessdenied.component';
import { NotFoundComponent } from './error/not-found/not-found.component';

@NgModule({
  imports: [
    RouterModule.forRoot(
      [
        ...publicRoute,
        {
          path: '',
          loadChildren: () => import(`./private/private.module`).then(({ PrivateModule }) => PrivateModule),
        },
        {
          path: 'error',
          component: ErrorComponent,
          title: 'error.title',
        },
        {
          path: 'accessdenied',
          component: AccessdeniedComponent,
          data: {
            errorMessage: 'error.http.403',
          },
          title: 'error.title',
        },
        {
          path: '404',
          component: NotFoundComponent,
          data: {
            errorMessage: 'error.http.404',
          },
          title: 'error.title',
        },
        {
          path: '**',
          component: NotFoundComponent
        },
      ],
      { enableTracing: environment.DEBUG_INFO_ENABLED, bindToComponentInputs: true, anchorScrolling: 'enabled' },
    ),
  ],
  exports: [RouterModule],
})
export class AppRoutingModule {}
