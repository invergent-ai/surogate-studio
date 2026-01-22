import { Component, OnInit } from '@angular/core';
import {ProgressSpinnerModule} from 'primeng/progressspinner';
import { ActivatedRoute, Router } from '@angular/router';
import {PaginatorModule} from 'primeng/paginator';
import {ReactiveFormsModule} from '@angular/forms';
import {InputTextModule} from 'primeng/inputtext';
import {PanelModule} from 'primeng/panel';
import {InputTextareaModule} from 'primeng/inputtextarea';
import {ButtonModule} from 'primeng/button';
import {RippleModule} from 'primeng/ripple';
import SharedModule from '../../../../../shared/shared.module';
import {IApplication} from '../../../../../shared/model/application.model';
import {PageComponent} from '../../../../../shared/components/page/page.component';
import {AppDetailsComponent} from "../../details/app-details.component";
import { CardModule } from 'primeng/card';
import { ApplicationMode } from '../../../../../shared/model/enum/application-mode.model';
import { LayoutService } from '../../../../../shared/service/theme/app-layout.service';

@Component({
  standalone: true,
  imports: [
    ProgressSpinnerModule,
    PaginatorModule,
    ReactiveFormsModule,
    InputTextModule,
    PanelModule,
    InputTextareaModule,
    ButtonModule,
    RippleModule,
    SharedModule,
    AppDetailsComponent,
    PageComponent,
    CardModule
  ],
  templateUrl: './deploy-step2.page.html',
})
export class DeployStep2Page implements OnInit {
  readonly welcomeItems = [
    { title: 'Surogate Concepts', link: 'https://surogate.ai/' },
    { title: 'Applications in Surogate', link: 'https://surogate.ai/' },
    { title: 'Create a new Application', link: 'https://docs.statemesh.net/applications/create' },
  ];

  mode: ApplicationMode = null;
  application: IApplication;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private layoutService: LayoutService,
  ) {}

  ngOnInit() {
    this.layoutService.state.helpItems = this.welcomeItems;
    this.route.queryParams.subscribe(params => {
      if (params['mode'] === ApplicationMode.MODEL) {
        this.mode = ApplicationMode.MODEL;
      } else {
        this.mode = ApplicationMode.APPLICATION;
      }
    });
  }

  async cancel() {
    await this.router.navigate(['/']);
  }
}
