import { Component, computed, inject, input, signal } from '@angular/core';
import { ButtonDirective } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DropdownModule } from 'primeng/dropdown';
import { LucideAngularModule, Plus, RefreshCw, Trash } from 'lucide-angular';
import {
  ILakeFsBranchCreation,
  ILakeFsRef,
  ILakeFsRepository,
  ILakeFsTagCreation
} from '../../../shared/model/lakefs.model';
import { derivedAsync } from 'ngxtension/derived-async';
import { catchError } from 'rxjs/operators';
import { displayError, displayErrorAndRethrow } from '../../../shared/util/error.util';
import { Store } from '@ngxs/store';
import { LakeFsService } from '../../../shared/service/lake-fs.service';
import { TableModule } from 'primeng/table';
import { TreeTableModule } from 'primeng/treetable';
import { TagModule } from 'primeng/tag';
import { NgIf } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RefSelection, RefSelectorComponent } from './ref-selector.component';
import { DialogModule } from 'primeng/dialog';
import SharedModule from '../../../shared/shared.module';
import { revalidateForm } from '../../../shared/util/form.util';
import { lastValueFrom } from 'rxjs';
import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';

@Component({
  selector: 'sm-hub-refs',
  standalone: true,
  templateUrl: './hub-refs.component.html',
  imports: [
    ButtonDirective,
    CardModule,
    DropdownModule,
    LucideAngularModule,
    TableModule,
    TreeTableModule,
    TagModule,
    NgIf,
    RouterLink,
    DialogModule,
    SharedModule,
    ReactiveFormsModule,
    RefSelectorComponent,
    ConfirmDialogModule
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
export class HubRefsComponent {
  type = input.required<'branch' | 'tag'>();
  repository = input.required<ILakeFsRepository>();
  typeLabel = computed(() => this.type() === 'branch' ? 'Branch' : 'Tag')
  typeLabelPlural = computed(() => this.type() === 'branch' ? 'Branches' : 'Tags')
  branchesOrTags = derivedAsync(() => {
    this.refreshRefs();
    if (this.type() === 'branch' && this.repository()) {
      return this.lakeFsService.listBranches(this.repository().id)
        .pipe(catchError((e) => displayErrorAndRethrow(this.store, e)))
    } else if (this.type() === 'tag' && this.repository()) {
      return this.lakeFsService.listTags(this.repository().id)
        .pipe(catchError((e) => displayErrorAndRethrow(this.store, e)))
    } else {
      return [];
    }
  })
  refreshRefs = signal(1);
  addBranchDialogVisible = signal(false);
  addTagDialogVisible = signal(false);

  addBranchForm = new FormGroup({
    name: new FormControl<string>(null, [Validators.required]),
    ref: new FormControl<RefSelection>(null, [Validators.required])
  });

  addTagForm = new FormGroup({
    name: new FormControl<string>(null, [Validators.required]),
    ref: new FormControl<RefSelection>(null, [Validators.required])
  });

  showAddBranchDialog() {
    this.addBranchDialogVisible.set(true);
    this.addBranchForm.reset({
      name: null,
      ref: null
    })
  }

  showAddTagDialog() {
    this.addTagDialogVisible.set(true);
    this.addTagForm.reset({
      name: null,
      ref: null
    })
  }

  refresh() {
    this.refreshRefs.set(new Date().getTime());
  }

  create() {
    this.type() === 'branch' ? this.showAddBranchDialog() : this.showAddTagDialog();
  }

  async createBranch() {
    revalidateForm(this.addBranchForm);

    try {
      const { name, ref } = this.addBranchForm.value;
      const branch = {
        name: name,
        source: ref.id,
        force: false
      } as ILakeFsBranchCreation;
      await lastValueFrom(this.lakeFsService.createBranch(this.repository().id, branch));
      this.refresh();
      this.addBranchDialogVisible.set(false);
    } catch (e) {
      displayError(this.store, e);
    }
  }

  async createTag() {
    revalidateForm(this.addTagForm);

    try {
      const { name, ref } = this.addTagForm.value;
      const tag = {
        id: name,
        ref: ref.id,
        force: false
      } as ILakeFsTagCreation;
      await lastValueFrom(this.lakeFsService.createTag(this.repository().id, tag));
      this.refresh();
      this.addTagDialogVisible.set(false);
    } catch (e) {
      displayError(this.store, e);
    }
  }

  async delete(event: Event, ref: ILakeFsRef) {
    this.confirmationService.confirm({
      target: event.target as EventTarget,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger p-button-sm',
      rejectButtonStyleClass: 'p-button-outlined p-button-sm',
      message: `Are you sure you want to delete this ${this.typeLabel()}?`,
      accept: async () => {
        try {
          if (this.type() === 'branch') {
            await lastValueFrom(this.lakeFsService.deleteBranch(this.repository().id, ref.id));
          } else {
            await lastValueFrom(this.lakeFsService.deleteTag(this.repository().id, ref.id));
          }
          this.refresh();
        } catch (e) {
          displayError(this.store, e);
        }
      }
    });
  }

  readonly confirmationService = inject(ConfirmationService);
  readonly lakeFsService = inject(LakeFsService);
  readonly store = inject(Store);

  protected readonly RefreshCw = RefreshCw;
  protected readonly Plus = Plus;
  protected readonly Trash = Trash;
}
