import {Component, inject, OnInit, signal} from '@angular/core';
import {PageComponent} from '../../../shared/components/page/page.component';
import {PageLoadComponent} from '../../../shared/components/page-load/page-load.component';
import {CardModule} from 'primeng/card';
import {ButtonDirective} from 'primeng/button';
import {CardComponent} from '../../../shared/components/card/card.component';
import {CheckboxModule} from 'primeng/checkbox';
import {DropdownModule} from 'primeng/dropdown';
import {InputNumberModule} from 'primeng/inputnumber';
import {InputTextModule} from 'primeng/inputtext';
import {InputTextareaModule} from 'primeng/inputtextarea';
import {
  JobNotificationSettingsComponent
} from '../components/job-notification-settings/job-notification-settings.component';
import {LabelTooltipComponent} from '../../../shared/components/label-tooltip/label-tooltip.component';
import {
  ArrowRight,
  ClipboardList,
  Database,
  Flame,
  LucideAngularModule,
  Plus,
  Save,
  Server,
  SlidersHorizontal,
  Trash
} from 'lucide-angular';
import {NgIf} from '@angular/common';
import {TableModule} from 'primeng/table';
import {TagModule} from 'primeng/tag';
import {
  BASE_CHECKPOINT,
  BASE_MODEL_REPOSITORY,
  BASE_NEW_BRANCH,
  BASE_RUN_NAME,
  DATASET_MAX_SEQ_LEN,
  DATASET_REPOSITORY,
  DATASET_SPLIT,
  DATASET_TEXT_COLUMN
} from '../tooltips';
import {LayoutService} from '../../../shared/service/theme/app-layout.service';
import {FormArray, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {DatasetTableChooserComponent} from '../components/dataset-table-chooser/dataset-table-chooser.component';
import {RepoSelectorComponent} from '../../../shared/components/repo-selector/repo-selector.component';
import {debugForm} from '../../../shared/util/form.util';

@Component({
  standalone: true,
  selector: 'sm-quantization-page',
  templateUrl: './quantization.page.html',
  imports: [
    PageComponent,
    PageLoadComponent,
    CardModule,
    ButtonDirective,
    CardComponent,
    CheckboxModule,
    DropdownModule,
    InputNumberModule,
    InputTextModule,
    InputTextareaModule,
    JobNotificationSettingsComponent,
    LabelTooltipComponent,
    LucideAngularModule,
    NgIf,
    TableModule,
    TagModule,
    FormsModule,
    ReactiveFormsModule,
    DatasetTableChooserComponent,
    RepoSelectorComponent
  ]
})
export class QuantizationPage implements OnInit {
  readonly welcomeItems = [
    { title: 'What is Surogate ?', link: 'https://surogate.ai/' },
    { title: 'Quantize models', link: 'https://surogate.ai/' },
  ];

  advanced = signal(false);

  form = new FormGroup({
    runName: new FormControl<string>(null, [Validators.required]),
    baseModel: new FormControl<string>(null, [Validators.required]),
    fromCheckpoint: new FormControl<boolean>(false),
    checkpoint: new FormControl<string>(null),
    newBranch: new FormControl<string>(null, [Validators.required]),
    description: new FormControl<string>(null),
    datasets: new FormArray<FormGroup>([]),
    maxSeqLength: new FormControl<number>(2048, [Validators.required]),
    notify: new FormControl<string[]>([]),
  });

  get datasets(): FormArray {
    return this.form.get('datasets') as FormArray;
  }


  ngOnInit() {
    this.layoutService.state.helpItems = this.welcomeItems;
  }

  async save() {
    console.log(this.form.value);
  }

  async launch() {
    console.log(this.form.value);
  }

  protected readonly layoutService = inject(LayoutService);
  protected readonly Database = Database;
  protected readonly Plus = Plus;
  protected readonly Trash = Trash;
  protected readonly Server = Server;
  protected readonly Flame = Flame;
  protected readonly ClipboardList = ClipboardList;
  protected readonly SlidersHorizontal = SlidersHorizontal;

  protected readonly BASE_RUN_NAME = BASE_RUN_NAME;
  protected readonly BASE_MODEL_REPOSITORY = BASE_MODEL_REPOSITORY;
  protected readonly BASE_NEW_BRANCH = BASE_NEW_BRANCH;
  protected readonly BASE_CHECKPOINT = BASE_CHECKPOINT;
  protected readonly DATASET_REPOSITORY = DATASET_REPOSITORY;
  protected readonly DATASET_SPLIT = DATASET_SPLIT;
  protected readonly DATASET_TEXT_COLUMN = DATASET_TEXT_COLUMN;
  protected readonly DATASET_MAX_SEQ_LEN = DATASET_MAX_SEQ_LEN;
  protected readonly Save = Save;
  protected readonly ArrowRight = ArrowRight;
  protected readonly debugForm = debugForm;
}
