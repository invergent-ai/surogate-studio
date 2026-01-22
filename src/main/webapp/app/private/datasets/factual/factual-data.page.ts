import {Component, inject, OnInit} from "@angular/core";
import {LayoutService} from "../../../shared/service/theme/app-layout.service";
import {CardModule} from "primeng/card";
import {CheckboxModule} from "primeng/checkbox";
import {DropdownModule} from "primeng/dropdown";
import {InputNumberModule} from "primeng/inputnumber";
import {InputTextModule} from "primeng/inputtext";
import {InputTextareaModule} from "primeng/inputtextarea";
import {Bot, ClipboardList, Columns3, FileText, Flame, LucideAngularModule, Settings} from "lucide-angular";
import {TableModule} from "primeng/table";
import {TagModule} from "primeng/tag";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {PageComponent} from "../../../shared/components/page/page.component";
import {PageLoadComponent} from "../../../shared/components/page-load/page-load.component";
import {CardComponent} from "../../../shared/components/card/card.component";
import {TabViewModule} from "primeng/tabview";
import {RadioButtonModule} from "primeng/radiobutton";
import {SliderModule} from "primeng/slider";
import {ChipsModule} from "primeng/chips";
import {FileUploaderComponent} from "../../../shared/components/file-upload/file-uploader.component";
import {Button, ButtonDirective} from "primeng/button";
import {NgIf} from "@angular/common";
import {StepperModule} from "primeng/stepper";
import {JobListComponent} from "../../../shared/components/jobs/job-list.component";
import {IJobType} from "../../../shared/model/job.model";
import {ExecutorType} from "../../../shared/model/enum/executor-type.model";

@Component({
  standalone: true,
  selector: 'sm-factual-data-page',
  templateUrl: './factual-data.page.html',
  imports: [
    CardModule,
    CheckboxModule,
    DropdownModule,
    InputNumberModule,
    InputTextModule,
    InputTextareaModule,
    LucideAngularModule,
    TableModule,
    TagModule,
    FormsModule,
    ReactiveFormsModule,
    PageComponent,
    PageLoadComponent,
    CardComponent,
    TabViewModule,
    RadioButtonModule,
    SliderModule,
    ChipsModule,
    FileUploaderComponent,
    ButtonDirective,
    NgIf,
    StepperModule,
    Button,
    JobListComponent,
  ]
})
export class FactualDataPage implements OnInit {
  readonly welcomeItems = [
    { title: 'What is Surogate ?', link: 'https://surogate.ai/' },
    { title: 'Generate Factual Data', link: 'https://invergent.ai' },
  ];

  chunkingStrategy: 'structure' | 'custom' | 'fixed' = 'structure';

  uploadedFiles = [
    { file: 'contract.pdf', type: 'PDF', pages: 24, size: '3.1 MB', status: 'converting' },
    { file: 'knowledge_base.md', type: 'MD', pages: 0, size: '142 KB', status: 'ready' },
  ];

  chunks = [
    { id: 'knowledge_base.md#ck1', source: 'knowledge_base.md', tokens: 488, text: ' DenseMAX Studio supports Git‑like dataset ops including branching, tagging, and diffs.' },
    { id: 'contract.pdf#ck9', source: 'contract.pdf', tokens: 520, text: 'Parties agree to the following service‑level objectives and penalties' },
  ];

  questions = [
    { id: 'knowledge_base.md#q1', text: 'What Git‑like dataset operations are supported by DenseMAX Studio?', source: 'knowledge_base.md#ck1', score: 0.87 },
    { id: 'contract.pdf#q1', text: 'Which service‑level objectives are defined in the example contract?', source: 'contract.pdf#ck9', score: 0.74 },
  ];

  answers = [
    { id: 'knowledge_base.md#a1', question: 'What Git‑like dataset operations are supported by DenseMAX Studio?', text: 'DenseMAX provides branching, tagging, pull requests, and diffs for datasets, plus import/export to popular hubs ...', tokens: 282, status: 'approved' },
    { id: 'contract.pdf#a1', question: 'Which service‑level objectives are defined in the example contract?', text: 'The contract defines uptime thresholds, response time targets, and penalty clauses tied to monthly availability ...', tokens: 361, status: 'rejected' },
  ];

  ngOnInit() {
    this.layoutService.state.helpItems = this.welcomeItems;
  }

  protected readonly layoutService = inject(LayoutService);
  protected readonly ClipboardList = ClipboardList;
  protected readonly Settings = Settings;
  protected readonly Flame = Flame;
  protected readonly FileText = FileText;
  protected readonly Columns3 = Columns3;
  protected readonly Bot = Bot;
  protected readonly IJobType = IJobType;
  protected readonly ExecutorType = ExecutorType;
}
