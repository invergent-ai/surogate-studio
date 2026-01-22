import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  computed,
  effect,
  inject,
  input,
  signal
} from '@angular/core';
import {LakeFsService} from "../../service/lake-fs.service";
import {ILakeFsRepository} from "../../model/lakefs.model";
import {OverlayPanelModule} from "primeng/overlaypanel";
import {DropdownModule} from "primeng/dropdown";
import {Button} from "primeng/button";
import {ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR} from "@angular/forms";
import {LucideAngularModule} from 'lucide-angular';
import {ListboxModule} from "primeng/listbox";
import {derivedAsync} from 'ngxtension/derived-async';
import {catchError, map} from 'rxjs/operators';
import {displayErrorAndRethrow} from '../../util/error.util';
import {Store} from '@ngxs/store';
import {repoDisplayNameOrId} from "../../util/naming.util";

@Component({
  selector: 'sm-repo-selector',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    OverlayPanelModule,
    DropdownModule,
    FormsModule,
    LucideAngularModule,
    Button,
    ListboxModule
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi:true,
      useExisting: RepoSelectorComponent
    }
  ],
  templateUrl: './repo-selector.component.html',
})
export class RepoSelectorComponent implements ControlValueAccessor {
  private lakeFsService = inject(LakeFsService);
  private cdr = inject(ChangeDetectorRef);
  private store = inject(Store);

  canSelect = input<boolean>(true);
  repoType = input<'model' | 'dataset' | 'any'>('any');
  maxDisplayLength = input<number>(20);
  repos = derivedAsync(() => this.lakeFsService.listRepositories()
    .pipe(
      map((repos=> {
        const filtered = this.repoType() === 'any' ? repos : repos.filter(r => r.metadata?.type === this.repoType());
        return filtered.map(repo => ({
          ...repo,
          displayLabel: repo.metadata?.displayName || repo.id
        }));
      })),
      catchError((e) => displayErrorAndRethrow(this.store, e)),
    ));
  selectedPath = signal<string | null>(null);
  // holds an incoming value until repos are loaded
  pendingRepoId = signal<string | null>(null);

  selectedLabel = computed(() => {
    const path = this.selectedPath();
    if (!path) {
      return "Select Repository...";
    }
    if (this.maxDisplayLength() && path.length > this.maxDisplayLength()) {
      return path.substring(0, this.maxDisplayLength()).concat('...');
    }
    return path;
  })

  selectedRepo: ILakeFsRepository | null = null;
  onChange: (value: any) => void = () => {};
  onTouched: () => void = () => {};

  constructor() {
    // Apply pending value once repos arrive
    effect(() => {
      const list = this.repos();
      const pending = this.pendingRepoId();
      if (list && pending) {
        const found = list.find(r => r.id === pending);
        if (found) {
          this.selectedRepo = found;
          this.selectedPath.set(repoDisplayNameOrId(this.selectedRepo));
          this.pendingRepoId.set(null);
          this.cdr.markForCheck();
        }
      }
    }, {allowSignalWrites: true});
  }

  onRepoChange(panel: any) {
    if (this.selectedRepo) {
      this.selectedPath.set(repoDisplayNameOrId(this.selectedRepo));
      this.onChange(this.selectedRepo.id);
      // Delay hiding to let change detection complete
      Promise.resolve().then(() => {
        panel.hide();
        this.cdr.markForCheck();
      });
    }
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
  }

  writeValue(value: any): void {
    const repos = this.repos();

    if (!value) {
      // Clear everything on null/undefined
      this.selectedRepo = null;
      this.selectedPath.set(null);
      this.pendingRepoId.set(null);
      return;
    }

    if (repos) {
      const found = repos.find(r => r.id === value);
      if (found) {
        this.selectedRepo = found;
        this.selectedPath.set(repoDisplayNameOrId(this.selectedRepo));
        this.pendingRepoId.set(null);
        return;
      }
      // Value not found in current list -> clear stale selection
      this.selectedRepo = null;
      this.selectedPath.set(null);
      this.pendingRepoId.set(value);
    } else {
      // Repos not loaded yet -> clear any stale selection, store pending
      this.selectedRepo = null;
      this.selectedPath.set(null);
      this.pendingRepoId.set(value);
    }
  }
}
