import {Routes} from '@angular/router';
import {UserRouteAccessService} from '../shared/service/user-route-access.service';
import RegisterComponent from './register/register.component';
import ActivateComponent from './activate/activate.component';
import PasswordComponent from './password/password.component';
import PasswordResetFinishComponent from './password-reset/finish/password-reset-finish.component';
import PasswordResetInitComponent from './password-reset/init/password-reset-init.component';
import LoginPage from './login/login-page/login.page';
import {JwtLoginPage} from './login/jwt-login-page/jwt-login.page';

const publicRoutes: Routes = [
  {
    path: 'auth',
    component: JwtLoginPage,
    title: 'routes.auth',
  },
  {
    path: 'login',
    component: LoginPage,
    title: 'routes.auth',
  },
  {
    path: 'activate',
    component: ActivateComponent,
    title: 'activate.title',
  },
  {
    path: 'password',
    component: PasswordComponent,
    title: 'global.menu.account.password',
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'reset/finish',
    component: PasswordResetFinishComponent,
    title: 'global.menu.account.password',
  },
  {
    path: 'reset/request',
    component: PasswordResetInitComponent,
    title: 'global.menu.account.password',
  },
  {
    path: 'register',
    component: RegisterComponent,
    title: 'register.title',
  }
];

export default publicRoutes;
