import {Component, computed, inject, input, signal, viewChild} from '@angular/core';
import {Button} from 'primeng/button';
import {OverlayPanel, OverlayPanelModule} from 'primeng/overlaypanel';
import {Selectors} from '../../../shared/state/selectors';
import {derivedAsync} from 'ngxtension/derived-async';
import {catchError, map} from 'rxjs/operators';
import {displayErrorAndRethrow} from '../../../shared/util/error.util';
import {Store} from '@ngxs/store';
import {LakeFsService} from '../../../shared/service/lake-fs.service';
import {DataViewModule} from 'primeng/dataview';
import {ControlValueAccessor, NG_VALUE_ACCESSOR} from '@angular/forms';
import {NgClass, NgIf} from '@angular/common';
import {InputTextModule} from 'primeng/inputtext';
import {TabViewModule} from 'primeng/tabview';
import {ChevronLeft, ChevronRight, LucideAngularModule} from 'lucide-angular';
import {TagModule} from 'primeng/tag';

export interface RefSelection {
  type: 'branch' | 'tag' | 'commit';
  id: string;
  metadata?: any;
}

@Component({
  selector: 'sm-ref-selector',
  standalone: true,
  templateUrl: './ref-selector.component.html',
  imports: [
    Button,
    OverlayPanelModule,
    DataViewModule,
    InputTextModule,
    TabViewModule,
    NgIf,
    NgClass,
    LucideAngularModule,
    TagModule
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: RefSelectorComponent
    }
  ]
})
export class RefSelectorComponent implements ControlValueAccessor {
  type = input.required<'branch' | 'tag' | 'commit' | 'any'>();
  repoId = input.required<string>();
  panel = viewChild(OverlayPanel);
  buttonSize = input<'large' | 'small'>('small');
  canSelect = input<boolean>(false);

  onlyTrainable = input<boolean>(false);
  onlyDeployable = input<boolean>(false);

  repository = computed(() => this.store.selectSignal(Selectors.repositoryById(this.repoId()))());

  branches = derivedAsync(() => this.repository() && (this.type() === 'branch' || this.type() === 'commit' || this.type() === 'any') &&
    this.lakeFsService.listBranches(this.repository().id)
      .pipe(
        map(branches => {
          if (this.onlyTrainable()) {
            return branches.filter(b => b.id === 'main' || (b.metadata?.source_model && b.metadata?.lora_adapter !== 'true'));
          }
          if (this.onlyDeployable()) {
            return branches.filter(b => b.id === 'main' || b.metadata?.source_model);
          }
          return branches;
        }),
        catchError((e) => displayErrorAndRethrow(this.store, e)),
      )
      .pipe(catchError((e) => displayErrorAndRethrow(this.store, e))) || []);

  tags = derivedAsync(() => this.repository() && (this.type() === 'tag' || this.type() === 'commit' || this.type() === 'any') &&
    this.lakeFsService.listTags(this.repository().id)
      .pipe(catchError((e) => displayErrorAndRethrow(this.store, e))) || []);

  commits = derivedAsync(() => {
    if (this.repository() && this.selectedRefForCommits()) {
      return this.lakeFsService.getCommits(this.repoId(), this.selectedRefForCommits())
        .pipe(catchError((e) => displayErrorAndRethrow(this.store, e)));
    }
    return [];
  });

  showBranches = computed(() => this.type() === 'branch' || this.type() === 'commit' || this.type() === 'any');
  showTags = computed(() => this.type() === 'tag' || this.type() === 'commit' || this.type() === 'any');
  showCommits = computed(() => this.type() === 'commit' || this.type() === 'any');

  // the selected value
  selectedValue = signal<RefSelection>(null);
  selectedRefForCommits = signal<string>(null);

  selectedValueLabel = computed(() => {
    const value = this.selectedValue();
    if (value) {
      switch (value.type) {
        case 'branch':
          return `Branch: ${value.id}`;
        case 'tag':
          return `Tag: ${value.id}`;
        case 'commit':
          return `Commit: ${value.id.substring(0, 9)}`;
      }
    }
    return 'Select Branch...'
  });

  onChange = (_: RefSelection) => {
  };
  onTouched: Function = () => {
  };

  writeValue(ref: RefSelection): void {
    this.selectedValue.set(ref);
  }

  setSelectedValue(source: 'branch' | 'tag' | 'commit', refId: string, metadata?: any): void {
    if (this.type() == 'any' || this.type() === source) {
      const value = {id: refId, type: source, metadata};
      // if any selection is provided, or if source matches control type, emit selected value
      this.selectedValue.set(value);
      this.onChange(value);
      this.panel().hide();
    }
  }

  navigateToCommits(source: 'branch' | 'tag', refId: string) {
    this.selectedRefForCommits.set(refId);
  }

  navigateBackToRefs() {
    this.selectedRefForCommits.set(null);
  }

  // When a form value changes due to user input, we need to report the value back to the parent form.
  // This is done by calling the fn callback
  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState?(isDisabled: boolean): void {
  }

  readonly store = inject(Store);
  readonly lakeFsService = inject(LakeFsService);
  protected readonly ChevronRight = ChevronRight;
  protected readonly ChevronLeft = ChevronLeft;
}
