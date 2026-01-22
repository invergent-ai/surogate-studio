import {FormArray, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {takeUntil} from 'rxjs/operators';
import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {CommonModule} from '@angular/common';
import {SharedModule} from 'primeng/api';
import {InputTextModule} from 'primeng/inputtext';
import {ButtonModule} from 'primeng/button';
import {AccordionModule} from 'primeng/accordion';
import {Subject} from 'rxjs';
import {NewEnvironmentVariable} from '../../../../../../../shared/model/environment-variable.model';
import {ApplicationFormService} from '../../../../../../../shared/service/form/application-form.service';
import {LayoutService} from "../../../../../../../shared/service/theme/app-layout.service";
import {TooltipModule} from "primeng/tooltip";
import {FileUploader, FileUploadModule} from "ng2-file-upload";

@Component({
  selector: 'sm-environment-variables',
  templateUrl: './environment-variables.component.html',
  styleUrls: ['./environment-variables.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    ReactiveFormsModule,
    InputTextModule,
    ButtonModule,
    AccordionModule,
    TooltipModule,
    FileUploadModule
  ]
})
export class EnvironmentVariablesComponent implements OnInit, OnDestroy {
  @Input() containerForm!: FormGroup;
  @Output() envVarsUpdated = new EventEmitter<NewEnvironmentVariable[]>();

  private destroy$ = new Subject<void>();
  uploader: FileUploader;

  constructor(
    public layoutService: LayoutService,
    private applicationFormService: ApplicationFormService) {}

  get envVars(): FormArray {
    return this.containerForm.get('envVars') as FormArray;
  }

  addEnvironmentVariable(): void {
    const envVarsArray = this.containerForm.get('envVars') as FormArray;
    envVarsArray.push(this.applicationFormService.createEnvironmentVariableForm());
    this.emitUpdatedEnvVars();
  }

  removeEnvironmentVariable(index: number): void {
    const envVarsArray = this.containerForm.get('envVars') as FormArray;
    envVarsArray.removeAt(index);
    this.emitUpdatedEnvVars();
  }

  private emitUpdatedEnvVars(): void {
    const envVarsArray = this.containerForm.get('envVars') as FormArray;
    const envVars = envVarsArray.controls.map(control => ({
      id: null,
      key: control.get('key')?.value,
      value: control.get('value')?.value
    }));

    this.envVarsUpdated.emit(envVars);
  }

  ngOnInit(): void {
    this.uploader = new FileUploader({
      url: '', // We'll handle upload manually
      autoUpload: false,
      maxFileSize: 1024 * 1024, // 1MB
      removeAfterUpload: true,
      itemAlias: 'file',
    });

    // Initialize environment variables array
    if (!this.containerForm.contains('envVars')) {
      this.containerForm.addControl('envVars', new FormArray([]));
    }

    const envVarsArray = this.containerForm.get('envVars') as FormArray;
    const existingEnvVars = this.containerForm.get('envVars')?.value || [];

    // Clear existing values
    while (envVarsArray.length) {
      envVarsArray.removeAt(0);
    }

    // Add existing or initialize empty
    if (existingEnvVars.length) {
      existingEnvVars.forEach((envVar: NewEnvironmentVariable) => {
        envVarsArray.push(this.applicationFormService.createEnvironmentVariableForm(envVar));
      });
    }

    // Subscribe to changes
    envVarsArray.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.emitUpdatedEnvVars();
    });
  }

  fileSelected(files: File[]) {
    if (!files?.length) {
      return;
    }

    const reader = new FileReader();
    reader.onload = (event) => {
      const result: string =  event?.target?.result as string;
      if (result) {
        const envVarsArray = this.containerForm.get('envVars') as FormArray;
        result.split('\n').forEach(row => {
          let columns = row.split(';');
          if (columns.length <= 1) {
            columns = row.split(',');
          }
          if (columns.length > 1) {
            envVarsArray.push(
              this.applicationFormService.createEnvironmentVariableForm({key: columns[0], value: columns[1]}));
          }
        });
      }
    };
    reader.readAsText(files[0]);
  }

  exportVariables(event: Event): void {
    event.stopPropagation();
    const blob = new Blob([this.convertEnvVarsToCSV()], {type: 'text/csv'});
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `env-vars-${new Date().toISOString()}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }

  private convertEnvVarsToCSV(): string {
    const rows = this.envVars.getRawValue().map(envVar => [
      envVar.key,
      envVar.value
    ]);

    return [
      ...rows.map(row => row.join(';'))
    ].join('\n');
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
