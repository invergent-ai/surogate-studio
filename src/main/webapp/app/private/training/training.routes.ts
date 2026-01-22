import {Routes} from '@angular/router';
import {UserRouteAccessService} from '../../shared/service/user-route-access.service';
import {TrainingWizardPage} from './wizard/training-wizard.page';
import {EvaluationPage} from './evaluation/evaluation.page';
import {QuantizationPage} from './quantization/quantization.page';
import {JobsPage} from './jobs/jobs.page';
import {TrainingPage} from "./training/training.page";

const trainingRoute: Routes = [
  {
    path: 'wizard',
    component: TrainingWizardPage,
    data: {
      breadcrumb: 'AI Factory Wizard'
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'evaluation',
    component: EvaluationPage,
    data: {
      breadcrumb: 'Evaluation'
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'evaluation/:id',
    component: EvaluationPage,
    data: {
      breadcrumb: 'Evaluation'
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'training/:type',
    canActivate: [UserRouteAccessService],
    data: { breadcrumb: 'Training' },
    children: [
      { path: '', component: TrainingPage },
      { path: ':id', component: TrainingPage },
    ],
  },
  {
    path: 'quantization',
    component: QuantizationPage,
    data: {
      breadcrumb: 'Quantization'
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'jobs/:executor/:type',
    component: JobsPage,
    data: {
      breadcrumb: 'Jobs'
    },
    canActivate: [UserRouteAccessService],
  },
];

export default trainingRoute;
