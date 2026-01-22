import { Component, OnInit } from '@angular/core';
import { TemplatesComponent } from '../../../../../shared/components/templates/templates.component';
import { PageComponent } from '../../../../../shared/components/page/page.component';
import { ApplicationMode } from '../../../../../shared/model/enum/application-mode.model';
import { LayoutService } from '../../../../../shared/service/theme/app-layout.service';

@Component({
  standalone: true,
  imports: [
    TemplatesComponent,
    PageComponent
  ],
  templateUrl: './deploy-step1.page.html',
  styles: [``]
})
export class DeployStep1Page implements OnInit {
  readonly welcomeItems = [
    { title: 'Surogate Concepts', link: 'https://surogate.ai/' },
    { title: 'Applications in Surogate', link: 'https://docs.statemesh.net/applications/intro' },
    { title: 'Create a new Application', link: 'https://docs.statemesh.net/applications/create' },
    { title: 'App Marketplace', link: 'https://docs.statemesh.net/applications/marketplace' },
  ];

  constructor(
    private layoutService: LayoutService,
  ) {}

  ngOnInit() {
    this.layoutService.state.helpItems = this.welcomeItems;
  }

  protected readonly ApplicationMode = ApplicationMode;
}
