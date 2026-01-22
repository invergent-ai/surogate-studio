import {Routes} from "@angular/router";
import {UserRouteAccessService} from "../../../shared/service/user-route-access.service";
import {UsersComponent} from "../components/users/users.component";
import {UserFormComponent} from "../components/users/user-form/user-form.component";

const usersRoute: Routes = [
  {
    path: '',
    component: UsersComponent,
    data: {
      breadcrumb: 'Users'
    },
    canActivate: [UserRouteAccessService],
  }, {
    path: 'add',
    component: UserFormComponent,
    data: {
      breadcrumb: 'Add Users'
    },
    canActivate: [UserRouteAccessService],
  },{
    path: 'edit/:login',
    component: UserFormComponent,
    data: {
      breadcrumb: 'Edit Users'
    },
    canActivate: [UserRouteAccessService],
  }
];

export default usersRoute;
