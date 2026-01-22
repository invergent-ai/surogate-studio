import {Component, Input, OnDestroy, OnInit} from "@angular/core";
import {DropdownModule} from "primeng/dropdown";
import {InputTextModule} from "primeng/inputtext";
import {FormGroup, ReactiveFormsModule} from "@angular/forms";
import {CommonModule} from "@angular/common";
import {AccordionModule} from "primeng/accordion";
import {DEFAULT_VALUES} from '../../../../../../../shared/model/container.model';
import {InputSwitchModule} from 'primeng/inputswitch';
import SharedModule from '../../../../../../../shared/shared.module';
import {InputNumberModule} from "primeng/inputnumber";
import {Subject} from "rxjs";

@Component({
  selector: 'sm-db-resource-settings',
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
export class ResourceSettingsComponent implements OnInit, OnDestroy {
  @Input() databaseForm!: FormGroup;

  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    // Set initial values if not already set
    const currentValues = this.databaseForm.value;
    this.databaseForm.patchValue({
      cpuLimit: currentValues.cpuLimit || DEFAULT_VALUES.CPU.limit,
      memLimit: currentValues.memLimit || DEFAULT_VALUES.MEMORY.limit
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
