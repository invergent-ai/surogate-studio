import { Component, HostListener, Input, OnDestroy, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AbstractControl, FormArray, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { SharedModule } from 'primeng/api';
import { AccordionModule } from 'primeng/accordion';
import { ChipsModule } from 'primeng/chips';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { CommonModule } from '@angular/common';
import { InputSwitchModule } from 'primeng/inputswitch';
import { Subject } from 'rxjs';
import { IVolume } from '../../../../../../shared/model/volume.model';
import { ApplicationFormService } from '../../../../../../shared/service/form/application-form.service';
import { VolumeType } from '../../../../../../shared/model/enum/volume-type.model';
import { NewVolumeMount } from '../../../../../../shared/model/volume-mount.model';
import { CheckboxModule } from 'primeng/checkbox';
import { TooltipModule } from 'primeng/tooltip';
import { LayoutService } from '../../../../../../shared/service/theme/app-layout.service';
import { Store } from '@ngxs/store';
import { Selectors } from '../../../../../../shared/state/selectors';
import { TagModule } from 'primeng/tag';

@Component({
  standalone: true,
  selector: 'sm-volume',
  styleUrls: ['./volume.component.scss'],
  templateUrl: './volume.component.html',
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    SharedModule,
    AccordionModule,
    ReactiveFormsModule,
    ChipsModule,
    DropdownModule,
    ButtonModule,
    InputNumberModule,
    InputSwitchModule,
    CheckboxModule,
    TooltipModule,
    TagModule
  ]
})
export class VolumeComponent implements OnInit, OnDestroy {
  @Input() containerForm!: FormGroup;
  @Input() applicationForm!: FormGroup;
  @Input() containerIndex: number
  @Input() defaultVolumes: string[] = [];
  @Input() resetTrigger!: Subject<void>;
  @Input() applicationId: string;

  private destroy$ = new Subject<void>();

  existingVolumes: IVolume[] = [];

  constructor(
    public layoutService: LayoutService,
    private applicationFormService: ApplicationFormService,
    private store: Store
  ) {
  }

  async ngOnInit() {
    if (!this.containerForm.contains('volumeMounts')) {
      this.containerForm.addControl('volumeMounts', new FormArray([]));
    }

    this.store.select(Selectors.volumes).subscribe(volumes =>
      this.existingVolumes = volumes.filter(volume => volume.project.id === this.applicationForm.getRawValue()?.project?.id)
    );
  }

  selectVolume(event: any, volumeMountControl: AbstractControl): void {
    if (volumeMountControl instanceof FormGroup) {
      const volumeId: string = event.value;
      const volume = this.existingVolumes.find(v => v.id === volumeId);
      volumeMountControl.patchValue({
        containerIndex: this.containerIndex,
        volume,
        volumeId: volumeId,
      });
    }
  }

  addVolumeMount(): void {
    const volumeMountsArray = this.containerForm.get('volumeMounts') as FormArray;
    const volumeMountForm = this.applicationFormService.createVolumeMountForm({
      volume: {
        id: null,
        type: VolumeType.TEMPORARY
      },
      containerIndex: this.containerIndex
    });
    volumeMountsArray.push(volumeMountForm);
    this.containerForm.markAsDirty();
  }

  removeVolumeMount(index: number): void {
    const volumesMountsArray = this.containerForm.get('volumeMounts') as FormArray;
    volumesMountsArray.removeAt(index);
    this.containerForm.markAsDirty();
  }

  getVolumeMountsArray(): FormArray {
    return this.containerForm.get('volumeMounts') as FormArray;
  }

  useDefaultVolumes() {
    if (!this.defaultVolumes?.length) {
      return; // Remove notification since volumes are being set automatically
    }

    const volumesArray = this.getVolumeMountsArray();
    // Don't clear if there are existing volumes
    const existingVolumes = volumesArray.controls.map(control =>
      control.get('containerPath')?.value
    );

    this.defaultVolumes.forEach(volumePath => {
      // Skip if volume path already exists
      if (existingVolumes.includes(volumePath)) {
        return;
      }

      const volumeMount: NewVolumeMount = {
        id: null,
        containerPath: volumePath,
        containerIndex: this.containerIndex,
        readOnly: false,
        volume: {
          id: null,
          type: VolumeType.TEMPORARY,
          name: volumePath.replace(/\//g, '-').substring(1)
        }
      };

      const volumeForm = this.applicationFormService.createVolumeMountForm(volumeMount);
      volumesArray.push(volumeForm);
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  protected readonly VolumeType = VolumeType;
}
