import {Component, inject, OnInit} from '@angular/core';
import {PageComponent} from '../../../shared/components/page/page.component';
import {PageLoadComponent} from '../../../shared/components/page-load/page-load.component';
import {CardModule} from 'primeng/card';
import {NgForOf, NgTemplateOutlet} from '@angular/common';
import {ApplicationMode} from '../../../shared/model/enum/application-mode.model';
import {ArrowRight, BrainCircuit, Database, FileText, Gauge, LucideAngularModule} from 'lucide-angular';
import {TagModule} from 'primeng/tag';
import {ButtonDirective} from 'primeng/button';
import {LayoutService} from '../../../shared/service/theme/app-layout.service';
import { Router, RouterLink } from '@angular/router';

@Component({
  standalone: true,
  selector: 'sm-data-wizard-page',
  templateUrl: './data-wizard.page.html',
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
export class DataWizardPage implements OnInit {
  readonly welcomeItems = [
    { title: 'What is Surogate ?', link: 'https://surogate.ai/' },
    { title: 'Types of AI Tasks', link: 'https://docs.statemesh.net/getting-started/overview' },
  ];

  readonly items: any[] = [
    {
      imgBg: "bg-purple-400",
      img: FileText,
      title: "Generate Factual Datasets",
      subTitle: "",
      description: "Extract training data from uploaded files. Automatically segment, clean, and label text for supervised fine-tuning.",
      tags: [
        {severity: "secondary", value: "real data", image: Database},
      ],
      estimated: "minutes / hours",
      routerLink: ['/data/factual'],
      disabled: true,
      enterprise: true
    },
    {
      imgBg: "bg-orange-500",
      img: BrainCircuit,
      title: "Generate Synthetic Datasets",
      subTitle: "",
      description: "Use a base model to generate high-quality synthetic instruction–response pairs for your target domain.",
      tags: [
        {severity: "secondary", value: "domain/task-specific", image: Database},
        {severity: "secondary", value: "instruction–response", image: Database},
        {severity: "secondary", value: "multi-turn", image: Database},
        {severity: "secondary", value: "function calling", image: Database},
        {severity: "secondary", value: "distillation", image: Database},
      ],
      estimated: "minutes / hours",
      routerLink: ['/data/synthetic'],
      disabled: true,
      enterprise: true
    },
    {
      imgBg: "bg-indigo-500",
      img: Gauge,
      title: "Generate Test Datasets",
      subTitle: "",
      description: "Compose test datasets to benchmark models across reasoning, factuality, and safety metrics.",
      tags: [
        {severity: "secondary", value: "instruction–response", image: Database},
        {severity: "secondary", value: "evaluation", image: Database},
      ],
      estimated: "minutes / hours",
      routerLink: ['/data/test'],
      disabled: true,
      enterprise: true
    },
  ];

  ngOnInit() {
    this.layoutService.state.helpItems = this.welcomeItems;
  }

  navigate(disabled: boolean, routerLink: string[]) {
    if (disabled) {
      return;
    }

    this.router.navigate(routerLink);
  }

  protected readonly router = inject(Router);
  protected readonly layoutService = inject(LayoutService);
  protected readonly ApplicationMode = ApplicationMode;
  protected readonly ArrowRight = ArrowRight;
}
