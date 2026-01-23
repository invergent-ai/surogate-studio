// src/app/shared/components/deployed-model-selector/deployed-model-selector.component.ts
import { Component, forwardRef, inject, Input } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { DropdownModule } from 'primeng/dropdown';
import { ApplicationService } from '../../service/application.service';
import { IApplication } from '../../model/application.model';
import { derivedAsync } from 'ngxtension/derived-async';
import { map } from 'rxjs/operators';
import { FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';

interface DeployedModelOption extends IApplication {
  apiModelName: string;
}

@Component({
  selector: 'sm-deployed-model-selector',
  standalone: true,
  imports: [DropdownModule, FormsModule, NgIf],
  template: `
    <p-dropdown
      [options]="deployedModels() ?? []"
      [ngModel]="value"
      (ngModelChange)="onChange($event)"
      optionLabel="name"
      [optionValue]="returnFull ? null : 'apiModelName'"
      [placeholder]="placeholder"
      [showClear]="true"
      [filter]="true"
      filterBy="name"
      appendTo="body"
      [style]="{ width: '100%' }"
    >
      <ng-template pTemplate="selectedItem" let-item>
        <div class="flex align-items-center gap-2" *ngIf="item">
          <span>{{ item.name }}</span>
          <span class="text-xs text-500">({{ item.deployedNamespace }})</span>
        </div>
      </ng-template>
      <ng-template pTemplate="item" let-item>
        <div class="flex align-items-center justify-content-between w-full">
          <span>{{ item.name }}</span>
          <span class="text-xs text-500">{{ item.deployedNamespace }}</span>
        </div>
      </ng-template>
    </p-dropdown>
  `,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DeployedModelSelectorComponent),
      multi: true,
    },
  ],
})
export class DeployedModelSelectorComponent implements ControlValueAccessor {
  @Input() placeholder = 'Select deployed model';
  @Input() returnFull = false;

  private applicationService = inject(ApplicationService);

  value: DeployedModelOption | string | null = null;
  private onChangeFn: (value: any) => void = () => {};
  private onTouchedFn: () => void = () => {};

  deployedModels = derivedAsync(() =>
    this.applicationService
      .query({ 'mode.equals': 'MODEL', 'status.equals': 'DEPLOYED' })
      .pipe(map(response => (response.body ?? []).map(app => this.withApiModelName(app)))),
  );

  private withApiModelName(app: IApplication): DeployedModelOption {
    const config = app.extraConfig ? JSON.parse(app.extraConfig) : {};
    const apiModelName = config.source === 'hf' ? config.hfModelName : config.branchToDeploy ?? config.modelName;
    return { ...app, apiModelName };
  }

  writeValue(value: any): void {
    this.value = value;
  }

  registerOnChange(fn: any): void {
    this.onChangeFn = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouchedFn = fn;
  }

  onChange(value: any): void {
    this.value = value;
    this.onChangeFn(value);
    this.onTouchedFn();
  }
}
