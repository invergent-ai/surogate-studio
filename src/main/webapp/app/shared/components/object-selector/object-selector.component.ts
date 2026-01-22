import {Component, computed, DestroyRef, effect, inject, input, OnInit, output, signal, viewChild} from '@angular/core';
import {LakeFsService} from "../../service/lake-fs.service";
import {ILakeFsObjectStats, ILakeFsRepository} from "../../model/lakefs.model";
import {TreeNode} from "primeng/api";
import {FileNavigatorComponent} from "../../../private/hub/components/file-navigator.component";
import {Button} from "primeng/button";
import {OverlayPanel, OverlayPanelModule} from "primeng/overlaypanel";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {ControlValueAccessor, NG_VALUE_ACCESSOR} from "@angular/forms";


@Component({
  selector: 'sm-object-selector',
  standalone: true,
  imports: [
    FileNavigatorComponent,
    Button,
    OverlayPanelModule
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi:true,
      useExisting: ObjectSelectorComponent
    }
  ],
  templateUrl: './object-selector.component.html',
  styleUrl: './object-selector.component.scss'
})
export class ObjectSelectorComponent implements ControlValueAccessor {
  repoId = input.required<string>();
  refId = input.required<string>();
  type = input<'file' | 'folder' | 'any'>('any');
  panel = viewChild(OverlayPanel);
  canSelect = input<boolean>(false);
  maxDisplayLength = input<number>(10);

  destroyRef = inject(DestroyRef);

  selectedPath = signal<string | null>(null);

  private onChange: (value: any) => void = () => {};
  private onTouched: () => void = () => {};

  selectedLabel = computed(() => {
    const path = this.selectedPath();
    if (!path) {
      return "Select...";
    }
    if (this.maxDisplayLength && path.length > this.maxDisplayLength()) {
      return path.substring(0, this.maxDisplayLength()).concat('...');
    }
    return path;
  })

  fakeRepository = computed(() => ({id: this.repoId()} as ILakeFsRepository));

  objects = signal<ILakeFsObjectStats[]>([]);

  constructor() {
    effect(() => {
      const repo = this.repoId();
      const ref = this.refId();

      if (!repo || !ref) {
        this.objects.set([]);
        this.onChange(null);
        return;
      }
      if (repo && ref) {
        this.selectedPath.set(null);
        this.onChange(null);
      }

      this.lakeFsService.listObjects(repo, ref)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(files => this.objects.set(files));
    }, {allowSignalWrites: true});

  }

  onSelectFile(node: TreeNode<ILakeFsObjectStats>) {
    const obj = node.data;
    if (!obj) return;
    const fullPath = `${this.repoId()}/${this.refId()}/${obj.path}`;
    this.panel().hide();
    this.selectedPath.set(`${obj.path}`);
    this.onChange(fullPath)
  }

  onSelectFolder(node: TreeNode) {
    if (!node?.data) return;
    const folderPath = (node.data.fullPath ?? '').replace(/\/$/, '');
    const fullPath = `${this.repoId()}/${this.refId()}/${folderPath}`;
    this.panel().hide();
    this.selectedPath.set(`${folderPath}`);
    this.onChange(fullPath);
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
  }

  writeValue(obj: any): void {
    if(!obj){
      this.selectedPath.set(null);
      return;
    }
    this.selectedPath.set(obj);
  }

  private lakeFsService = inject(LakeFsService);
}
