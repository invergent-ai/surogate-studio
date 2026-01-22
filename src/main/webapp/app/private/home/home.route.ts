import {Routes} from "@angular/router";
import {HomePage} from "./home.page";

const homeRoutes: Routes = [
  {
    path: '',
    data: {
      pageTitle: 'Home',
      breadcrumb: 'Home'
    },
    component: HomePage
  }
]

export default homeRoutes;
