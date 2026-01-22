import {Component, computed, inject, signal} from '@angular/core';
import {PageComponent} from '../../../shared/components/page/page.component';
import {PageLoadComponent} from '../../../shared/components/page-load/page-load.component';
import {injectParams} from 'ngxtension/inject-params';
import {InputTextModule} from 'primeng/inputtext';
import {DropdownModule} from 'primeng/dropdown';
import {Copy, LucideAngularModule, Plus, Scale, SlidersHorizontal, Trash} from 'lucide-angular';
import {TableModule} from 'primeng/table';
import {TagModule} from 'primeng/tag';
import {Router} from '@angular/router';
import {OverlayPanelModule} from 'primeng/overlaypanel';
import {Store} from '@ngxs/store';
import {IJobType} from "../../../shared/model/job.model";
import {ExecutorType} from "../../../shared/model/enum/executor-type.model";
import {JobListComponent} from "../../../shared/components/jobs/job-list.component";
import {ProgressBarModule} from "primeng/progressbar";
import {ButtonDirective} from "primeng/button";
import {TrainingMetricsComponent} from "../components/training-metrics/training-metrics.component";
import {TestChatComponent} from "../components/test-chat/test-chat.component";
import { EvaluationResultsComponent } from '../../../shared/components/evaluation-results/evaluation-results.component';

export type JobType = 'dataset' | 'training' | 'fine-tuning' | 'evaluation' | 'quantization';

@Component({
  standalone: true,
  selector: 'sm-jobs-age',
  templateUrl: './jobs.page.html',
  imports: [
    PageComponent,
    PageLoadComponent,
    InputTextModule,
    DropdownModule,
    LucideAngularModule,
    TableModule,
    TagModule,
    OverlayPanelModule,
    JobListComponent,
    ProgressBarModule,
    ButtonDirective,
    TrainingMetricsComponent,
    TestChatComponent,
    EvaluationResultsComponent,
  ],
})
export class JobsPage {
  executor = injectParams('executor');
  type = injectParams('type');
  jobSnapshotTime = signal(Date.now());
  enterprise = computed(() => {
    return this.type() === 'quantization';
  });
  pageTitle = computed(() => {
    switch (this.type() as JobType) {
      case 'dataset':
        return 'Dataset';
      case 'training':
        return 'Training';
      case 'fine-tuning':
        return 'Fine-Tuning';
      case 'evaluation':
        return 'Evaluation';
      case 'quantization':
        return 'Quantization';
      default:
        return '';
    }
  });
  jobTypes = computed(() => {
    switch (this.type()) {
      case 'dataset':
        return [IJobType.IMPORT_HF_DATASET, IJobType.IMPORT_HF_MODEL];
      case 'training':
        return [IJobType.TRAIN];
      case 'fine-tuning':
        return [IJobType.FINE_TUNE];
      case 'evaluation':
        return [IJobType.EVALUATION];
      case 'quantization':
        return [IJobType.QUANTIZATION];
      default:
        return [];
    }
  });
  editRoute = computed(() => {
    switch (this.type()) {
      case 'dataset':
        return null;
      case 'training':
        return '/train/training/pretrain';
      case 'fine-tuning':
        return '/train/training/finetune';
      case 'evaluation':
        return '/train/evaluation'; // <-- Add this
      case 'quantization':
        return null;
      default:
        return null;
    }
  });
  executorType = computed(() => {
    return this.executor() === 'ray' ? ExecutorType.RAY : ExecutorType.TEKTON;
  });

  async createJob(_: MouseEvent) {
    switch (this.type() as JobType) {
      case 'training': {
        await this.router.navigate(['/train/training/pretrain']);
        break;
      }
      case 'fine-tuning': {
        await this.router.navigate(['/train/training/finetune']);
        break;
      }
      case 'evaluation': {
        await this.router.navigate(['/train/evaluation']);
        break;
      }
      case 'quantization': {
        await this.router.navigate(['/train/quantization']);
        break;
      }
    }
  }

  protected readonly router = inject(Router);
  protected readonly store = inject(Store);

  protected readonly Plus = Plus;
  protected readonly Trash = Trash;
  protected readonly Copy = Copy;
  protected readonly SlidersHorizontal = SlidersHorizontal;
  protected readonly Scale = Scale;
}
