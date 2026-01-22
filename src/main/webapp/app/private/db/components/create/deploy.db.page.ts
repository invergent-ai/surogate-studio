import {Component} from '@angular/core';
import {ProgressSpinnerModule} from 'primeng/progressspinner';
import {Router} from '@angular/router';
import {PaginatorModule} from 'primeng/paginator';
import {ReactiveFormsModule} from '@angular/forms';
import {InputTextModule} from 'primeng/inputtext';
import {PanelModule} from 'primeng/panel';
import {InputTextareaModule} from 'primeng/inputtextarea';
import {ButtonModule} from 'primeng/button';
import {RippleModule} from 'primeng/ripple';
import SharedModule from '../../../../shared/shared.module';
import {PageComponent} from '../../../../shared/components/page/page.component';
import {CardModule} from 'primeng/card';
import {IDatabase} from "../../../../shared/model/database.model";
import {DbDetailsComponent} from "../details/db-details.component";

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
    DbDetailsComponent,
    PageComponent,
    CardModule
  ],
  templateUrl: './deploy.db.page.html',
})
export class DeployDbPage {
  readonly welcomeItems = [
    // { title: 'StateMesh Concepts', link: 'https://docs.statemesh.net/getting-started/concepts' },
    // { title: 'Applications in StateMesh', link: 'https://docs.statemesh.net/applications/intro' },
    { title: 'Create a new Application', link: 'https://docs.statemesh.net/applications/create' }
  ];

  database: IDatabase;

  constructor(
    private router: Router,
  ) {}

  async cancel() {
    await this.router.navigate(['/']);
  }
}
