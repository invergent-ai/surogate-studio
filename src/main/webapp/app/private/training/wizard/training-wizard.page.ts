import { Component, inject, OnInit } from '@angular/core';
import { PageComponent } from '../../../shared/components/page/page.component';
import { PageLoadComponent } from '../../../shared/components/page-load/page-load.component';
import { CardModule } from 'primeng/card';
import { NgForOf, NgTemplateOutlet } from '@angular/common';
import { ApplicationMode } from '../../../shared/model/enum/application-mode.model';
import {
  ArrowRight,
  Cpu,
  Database,
  Gauge,
  LucideAngularModule,
  Medal,
  Rocket,
  ScanText, ScrollText,
  SlidersHorizontal,
  Trophy
} from 'lucide-angular';
import { TagModule } from 'primeng/tag';
import { ButtonDirective } from 'primeng/button';
import { LayoutService } from '../../../shared/service/theme/app-layout.service';
import { RouterLink } from '@angular/router';

@Component({
  standalone: true,
  selector: 'sm-training-wizard-page',
  templateUrl: './training-wizard.page.html',
  imports: [
    PageComponent,
    PageLoadComponent,
    CardModule,
    NgTemplateOutlet,
    LucideAngularModule,
    TagModule,
    ButtonDirective,
    NgForOf,
    RouterLink
  ]
})
export class TrainingWizardPage implements OnInit {
  readonly welcomeItems = [
    { title: 'What is Surogate ?', link: 'https://surogate.ai/' },
    { title: 'Types of AI Tasks', link: 'https://surogate.ai/' },
  ];

  readonly items: any[] = [
    {
      imgBg: "bg-purple-400",
      img: ScrollText,
      title: "Data Factory",
      subTitle: "create training/evaluation data",
      description: "Generate datasets from documents, labeling, or distillation to support other training or fine-tuning tasks.",
      tags: [
        {severity: "secondary", value: "real/synthetic data", image: Database},
        {severity: "secondary", value: "domain-specific", image: Database},
        {severity: "secondary", value: "evaluation", image: Database},
        {severity: "success", value: "light", image: Gauge}
      ],
      estimated: "minutes / hours",
      routerLink: ['/data/wizard']
    },
    {
      imgBg: "bg-orange-500",
      img: Rocket,
      title: "Training",
      subTitle: "Train a base model",
      description: "Create a base model by training it on large raw text datasets.",
      tags: [
        {severity: "secondary", value: "raw text", image: Database},
        {severity: "danger", value: "heavy", image: Gauge}
      ],
      estimated: "weeks / months",
      routerLink: ['/train/training/pretrain'],
    },
    {
      imgBg: "bg-indigo-500",
      img: SlidersHorizontal,
      title: "Fine-Tuning",
      subTitle: "Continued training + SFT / LoRA / QLora",
      description: "Adapt a base checkpoint to your task using labeled data. Supports SFT/LoRA/QLoRA and Continued training.",
      tags: [
        {severity: "secondary", value: "instruction–response", image: Database},
        {severity: "secondary", value: "domain/task-specific", image: Database},
        {severity: "warning", value: "moderate", image: Gauge}
      ],
      estimated: "hours / days",
      routerLink: ['/train/training/finetune']
    },
    {
      imgBg: "bg-blue-400",
      img: Trophy,
      title: "Evaluation",
      subTitle: "test performance / security",
      description: "Run automatic benchmarks (e.g., MMLU, ARC-AGI, IF‑Bench), security (PII, Safety, Privacy) and custom evals.",
      tags: [
        {severity: "secondary", value: "benchmarks", image: Database},
        {severity: "success", value: "light", image: Gauge}
      ],
      estimated: "minutes / hours",
      routerLink: ['/train/evaluation']
    },
    {
      imgBg: "bg-teal-400",
      img: Cpu,
      title: "Quantization",
      subTitle: "compress model",
      description: "Shrink model size and speed up inference by lowering numeric precision.",
      tags: [
        {severity: "secondary", value: "calibration data", image: Database},
        {severity: "danger", value: "high", image: Gauge}
      ],
      estimated: "minutes / hours",
      enterprise: true,
      disabled: true,
      routerLink: ['/train/quantization']
    },
  ];

  ngOnInit() {
    this.layoutService.state.helpItems = this.welcomeItems;
  }

  protected readonly layoutService = inject(LayoutService);
  protected readonly ApplicationMode = ApplicationMode;
  protected readonly ArrowRight = ArrowRight;
}
