import {Component, computed, inject, signal} from '@angular/core';
import {Bot, LucideAngularModule, Plus, RefreshCw, Trash} from 'lucide-angular';
import {PageComponent} from '../../shared/components/page/page.component';
import {PageLoadComponent} from '../../shared/components/page-load/page-load.component';
import {ConfirmationService, MenuItem, PrimeTemplate} from 'primeng/api';
import {TabViewModule} from 'primeng/tabview';
import {TagModule} from 'primeng/tag';
import {injectParams} from 'ngxtension/inject-params';
import {Selectors} from '../../shared/state/selectors';
import {Store} from '@ngxs/store';
import {LayoutService} from '../../shared/service/theme/app-layout.service';
import {TabMenuModule} from 'primeng/tabmenu';
import {HubRefsComponent} from './components/hub-refs.component';
import {hubMenu} from './menu';
import {CardModule} from "primeng/card";
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {displayError} from "../../shared/util/error.util";
import {lastValueFrom} from "rxjs";
import {LakeFsService} from "../../shared/service/lake-fs.service";
import {DialogModule} from 'primeng/dialog';

import {ConfirmDialogModule} from "primeng/confirmdialog";
import {InputTextModule} from "primeng/inputtext";
import {NgIf} from "@angular/common";
import {TableModule} from "primeng/table";
import {derivedAsync} from "ngxtension/derived-async";
import {User} from "../../shared/model/user.model";

@Component({
  selector: 'sm-page-settings',
  standalone: true,
  templateUrl: './settings.page.html',
  imports: [
    LucideAngularModule,
    PageComponent,
    PageLoadComponent,
    PrimeTemplate,
    TabViewModule,
    TagModule,
    TabMenuModule,
    HubRefsComponent,
    CardModule,
    ReactiveFormsModule,
    ConfirmDialogModule,
    DialogModule,
    InputTextModule,
    NgIf,
    TableModule
  ],
  styles: `
    ::ng-deep .p-card-body {
      padding: 1rem !important;
    }

    ::ng-deep .p-dialog-header {
      padding-bottom: 1rem !important;
    }
  `
})
export class SettingsPage {
  repoId = injectParams('repo');
  lakeFsService = inject(LakeFsService);
  repository = computed(() => this.store.selectSignal(Selectors.repositoryById(this.repoId()))());
  menu: MenuItem[] = hubMenu(this.repoId());
  readonly confirmationService = inject(ConfirmationService);

  readonly store = inject(Store);
  readonly layoutService = inject(LayoutService);
  protected readonly Bot = Bot;
  protected readonly Plus = Plus;

  addUserDialogVisible = signal(false);
  refreshTrigger = signal(0);

  addUserForm = new FormGroup({
    userId: new FormControl('', Validators.required)
  });

  showAddUserDialog() {
    this.addUserForm.reset();
    this.addUserDialogVisible.set(true);
  }

  async createUser() {
    const {userId} = this.addUserForm.value;
    try {
      await lastValueFrom(
        this.lakeFsService.addUserToRepoGroup(this.repository().id, userId)
      );
      this.refreshTrigger.update(n => n + 1);
      this.addUserDialogVisible.set(false);
    } catch (e) {
      displayError(this.store, e);
    }
  }

  async deleteUser(event: Event, data: User) {
    this.confirmationService.confirm({
      target: event.target as EventTarget,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger p-button-sm',
      rejectButtonStyleClass: 'p-button-outlined p-button-sm',
      message: `Are you sure you want to remove user: ${data.id}?`,
      accept: async () => {
        try {
          await lastValueFrom(this.lakeFsService.deleteGroupMember("repo-" + this.repository().id, data.id));
          this.refreshTrigger.update(n => n + 1);
        } catch (e) {
          displayError(this.store, e);
        }
      }
    });
  }

  groupMembers = derivedAsync(() => {
      this.refreshTrigger();
      return this.lakeFsService.getGroupMembers("repo-" + this.repoId())
    }
  );

  protected readonly RefreshCw = RefreshCw;
  protected readonly Trash = Trash;
}
