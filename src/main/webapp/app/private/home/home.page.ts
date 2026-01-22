import { Component, OnInit } from '@angular/core';
import { Account } from '../../shared/model/account.model';
import { AccountService } from '../../shared/service/account.service';
import { PanelModule } from 'primeng/panel';
import { ButtonModule } from 'primeng/button';
import { PageComponent } from '../../shared/components/page/page.component';
import { RouterLink } from '@angular/router';
import { ResourceListComponent } from './components/resource-list/resource-list.component';
import { NodeListComponent } from './components/node-list/node-list.component';
import { environment } from '../../../environments/environment';
import { LayoutService } from '../../shared/service/theme/app-layout.service';
import { LucideAngularModule, SquarePlus } from 'lucide-angular';

@Component({
  standalone: true,
  imports: [
    PanelModule,
    ButtonModule,
    PageComponent,
    RouterLink,
    ResourceListComponent,
    NodeListComponent,
    LucideAngularModule
  ],
  templateUrl: './home.page.html'
})
export class HomePage implements OnInit {
  readonly helpItems = [
    { title: 'What is Surogate ?', link: 'https://surogate.ai/' },
    { title: 'Surogate Concepts', link: 'https://surogate.ai/' },
    { title: 'Applications in Surogate', link: 'https://docs.statemesh.net/applications/intro' }
  ];

  user: Account;

  constructor(
    private accountService: AccountService,
    private layoutService: LayoutService,
  ) {
  }

  async ngOnInit() {
    this.layoutService.state.helpItems = this.helpItems;

    this.accountService.identity().subscribe(account => {
      if (account) {
        this.user = account;
      }
    });
  }

  protected readonly environment = environment;
  protected readonly SquarePlus = SquarePlus;
}
