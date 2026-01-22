import {Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges} from "@angular/core";
import {DropdownModule} from "primeng/dropdown";
import {InputTextModule} from "primeng/inputtext";
import {FormControl, FormGroup, ReactiveFormsModule} from "@angular/forms";
import {CommonModule} from "@angular/common";
import {AccordionModule} from "primeng/accordion";
import {DEFAULT_VALUES, ResourceType} from '../../../../../../../shared/model/container.model';
import {ApplicationFormService} from "../../../../../../../shared/service/form/application-form.service";
import {InputSwitchModule} from 'primeng/inputswitch';
import SharedModule from '../../../../../../../shared/shared.module';
import {InputNumberModule} from "primeng/inputnumber";
import {takeUntil} from "rxjs/operators";
import {Subject} from "rxjs";

@Component({
  selector: 'sm-resource-settings',
  templateUrl: './resource-settings.component.html',
  styleUrls: ['./resource-settings.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    InputTextModule,
    DropdownModule,
    AccordionModule,
    InputSwitchModule,
    SharedModule,
    InputNumberModule
  ]
})
export class ResourceSettingsComponent implements OnInit, OnChanges, OnDestroy {
  @Input() containerForm!: FormGroup;
  showForm = false;

  gpuTypes = [
    {label: 'NVIDIA GPU', value: 'nvidia'}
  ];

  // resourceTypes = Object.values(ResourceType).map(value => ({
  //   label: value,
  //   value: value
  // }));
  private destroy$ = new Subject<void>();

  constructor(private applicationFormService: ApplicationFormService) {}

  getColumnClass(): string {
    // const resourceType = this.containerForm.get('resourceType')?.value;
    return 'col-6'; //resourceType === ResourceType.BOTH ? 'col-4' : 'col-6'; // Uncomment when needed
  }

  showCPUSettings(): boolean {
    const resourceType = this.containerForm.get('resourceType')?.value;
    return resourceType === ResourceType.CPU;// || resourceType === ResourceType.BOTH; // Uncomment when needed
  }

  showGPUSettings(): boolean {
    // const resourceType = this.containerForm.get('resourceType')?.value;
    return false; // resourceType === ResourceType.GPU || resourceType === ResourceType.BOTH; // Uncomment when needed
  }

  onResourceTypeChange(): void {
    const resourceType = this.containerForm.get('resourceType')?.value;
    if (resourceType) {
      this.applicationFormService.handleResourceTypeChange(this.containerForm, resourceType);
      // Ensure form is marked as dirty
      this.containerForm.markAsDirty();
    }
  }

  ngOnInit(): void {
    if (!this.containerForm.get('resourceType')) {
      this.containerForm.addControl('resourceType', new FormControl(ResourceType.CPU));
    }

    // Set initial values if not already set
    const currentValues = this.containerForm.value;
    this.containerForm.patchValue({
      resourceType: currentValues.resourceType || ResourceType.CPU,
      memRequest: currentValues.memRequest || DEFAULT_VALUES.MEMORY.request,
      memLimit: currentValues.memLimit || DEFAULT_VALUES.MEMORY.limit,
      cpuRequest: currentValues.cpuRequest || DEFAULT_VALUES.CPU.request,
      cpuLimit: currentValues.cpuLimit || DEFAULT_VALUES.CPU.limit,
      gpuLimit: currentValues.gpuLimit || 0
    });

    // Subscribe to resource type changes
    this.containerForm.get('resourceType')?.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(value => {
      if (value) {
        this.onResourceTypeChange();
      }
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes && changes.containerForm) {
      this.showForm = !!(this.containerForm.value?.cpuRequest !== DEFAULT_VALUES.CPU.request ||
        this.containerForm?.value.cpuLimit !== DEFAULT_VALUES.CPU.limit ||
        this.containerForm?.value.memRequest !== DEFAULT_VALUES.MEMORY.request ||
        this.containerForm?.value.memLimit !== DEFAULT_VALUES.MEMORY.limit);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
