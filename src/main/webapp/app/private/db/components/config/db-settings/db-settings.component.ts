import {Component, Input} from '@angular/core';
import {FormGroup, FormsModule, ReactiveFormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';
import {SharedModule} from 'primeng/api';
import {InputTextModule} from 'primeng/inputtext';
import {TooltipModule} from 'primeng/tooltip';
import {DropdownModule} from 'primeng/dropdown';
import {Subject} from 'rxjs';
import {AutoCompleteModule} from 'primeng/autocomplete';
import {InputSwitchModule} from 'primeng/inputswitch';
import {ButtonModule} from 'primeng/button';
import {VolumeComponent} from './volume/volume.component';
import {AccordionModule} from "primeng/accordion";
import {AdvancedSettingsComponent} from "./advanced-settings/advanced-settings.component";

@Component({
  selector: 'sm-db-settings',
  templateUrl: './db-settings.component.html',
  styleUrls: ['./db-settings.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    ReactiveFormsModule,
    InputTextModule,
    DropdownModule,
    TooltipModule,
    AutoCompleteModule,
    InputSwitchModule,
    FormsModule,
    ButtonModule,
    AccordionModule,
    AdvancedSettingsComponent,
    VolumeComponent
  ]
})
export class DbSettingsComponent {
  @Input() databaseForm!: FormGroup;
  @Input() databaseId!: string;
  @Input() resetTrigger!: Subject<void>;

  constructor() {}
}
