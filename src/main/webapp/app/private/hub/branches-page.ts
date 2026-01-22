import { Component, computed, inject } from '@angular/core';
import { Bot, LucideAngularModule } from 'lucide-angular';
import { PageComponent } from '../../shared/components/page/page.component';
import { PageLoadComponent } from '../../shared/components/page-load/page-load.component';
import { MenuItem, PrimeTemplate } from 'primeng/api';
import { TabViewModule } from 'primeng/tabview';
import { TagModule } from 'primeng/tag';
import { injectParams } from 'ngxtension/inject-params';
import { Selectors } from '../../shared/state/selectors';
import { Store } from '@ngxs/store';
import { LayoutService } from '../../shared/service/theme/app-layout.service';
import { TabMenuModule } from 'primeng/tabmenu';
import { HubRefsComponent } from './components/hub-refs.component';
import { hubMenu } from './menu';

@Component({
  selector: 'sm-page-branches',
  standalone: true,
  templateUrl: './branches.page.html',
  imports: [
    LucideAngularModule,
    PageComponent,
    PageLoadComponent,
    PrimeTemplate,
    TabViewModule,
    TagModule,
    TabMenuModule,
    HubRefsComponent
  ]
})
export class BranchesPage {
  repoId = injectParams('repo');
  repository = computed(() => this.store.selectSignal(Selectors.repositoryById(this.repoId()))());
  menu: MenuItem[] = hubMenu(this.repoId());

  readonly store = inject(Store);
  readonly layoutService = inject(LayoutService);
  protected readonly Bot = Bot;
}
