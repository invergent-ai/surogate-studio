import {Routes} from "@angular/router";
import {UserRouteAccessService} from "../../shared/service/user-route-access.service";
import {DataWizardPage} from "./wizard/data-wizard.page";
import {FactualDataPage} from "./factual/factual-data.page";
import {DataTasksPage} from "./data-tasks/data-tasks.page";

const datasetRoutes: Routes = [
  {
    path: 'wizard',
    component: DataWizardPage,
    data: {
      breadcrumb: 'AI Data Wizard'
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'factual',
    component: FactualDataPage,
    data: {
      breadcrumb: 'Factual Data'
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'tasks/:type',
    component: DataTasksPage,
    data: {
      breadcrumb: 'Data Tasks'
    },
    canActivate: [UserRouteAccessService],
  },
];

export default datasetRoutes;
