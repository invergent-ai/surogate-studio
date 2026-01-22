import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {RouterModule} from '@angular/router';
import {AbstractControl, FormArray, FormGroup, FormsModule, ReactiveFormsModule} from '@angular/forms';
import {SharedModule} from 'primeng/api';
import {AccordionModule} from 'primeng/accordion';
import {ChipsModule} from 'primeng/chips';
import {DropdownModule} from 'primeng/dropdown';
import {ButtonModule} from 'primeng/button';
import {InputNumberModule} from 'primeng/inputnumber';
import {CommonModule} from '@angular/common';
import {InputSwitchModule} from 'primeng/inputswitch';
import {Subject} from 'rxjs';
import {IVolume} from '../../../../../../shared/model/volume.model';
import {VolumeType} from '../../../../../../shared/model/enum/volume-type.model';
import {CheckboxModule} from 'primeng/checkbox';
import {TooltipModule} from 'primeng/tooltip';
import {LayoutService} from '../../../../../../shared/service/theme/app-layout.service';
import {Store} from '@ngxs/store';
import {Selectors} from '../../../../../../shared/state/selectors';
import {TagModule} from 'primeng/tag';
import {DatabaseFormService} from "../../../../../../shared/service/form/database-form.service";

@Component({
  standalone: true,
  selector: 'sm-db-volume',
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
  @Input() databaseForm!: FormGroup;
  @Input() resetTrigger!: Subject<void>;
  @Input() databaseId: string;

  private destroy$ = new Subject<void>();

  existingVolumes: IVolume[] = [];

  constructor(
    public layoutService: LayoutService,
    private databaseFormService: DatabaseFormService,
    private store: Store
  ) {}

  async ngOnInit() {
    if (!this.databaseForm.contains('volumeMounts')) {
      this.databaseForm.addControl('volumeMounts', new FormArray([]));
    }

    this.store.select(Selectors.volumes).subscribe(volumes =>
      this.existingVolumes = volumes.filter(volume => volume.project.id === this.databaseForm.getRawValue()?.project?.id)
    );
  }

  selectVolume(event: any, volumeMountControl: AbstractControl): void {
    if (volumeMountControl instanceof FormGroup) {
      const volumeId: string = event.value;
      const volume = this.existingVolumes.find(v => v.id === volumeId);
      volumeMountControl.patchValue({
        volume,
        volumeId: volumeId,
      });
    }
  }

  addVolumeMount(): void {
    const volumeMountsArray = this.databaseForm.get('volumeMounts') as FormArray;
    const volumeMountForm = this.databaseFormService.createVolumeMountForm({
      volume: {
        id: null,
        type: VolumeType.TEMPORARY
      }
    });
    volumeMountsArray.push(volumeMountForm);
    this.databaseForm.markAsDirty();
  }

  removeVolumeMount(index: number): void {
    const volumesMountsArray = this.databaseForm.get('volumeMounts') as FormArray;
    volumesMountsArray.removeAt(index);
    this.databaseForm.markAsDirty();
  }

  getVolumeMountsArray(): FormArray {
    return this.databaseForm.get('volumeMounts') as FormArray;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  protected readonly VolumeType = VolumeType;
}
