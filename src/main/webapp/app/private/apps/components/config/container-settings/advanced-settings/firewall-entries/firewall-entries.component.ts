import {FormArray, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {takeUntil} from 'rxjs/operators';
import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {CommonModule} from '@angular/common';
import {SharedModule} from 'primeng/api';
import {InputTextModule} from 'primeng/inputtext';
import {ButtonModule} from 'primeng/button';
import {AccordionModule} from 'primeng/accordion';
import {Subject} from 'rxjs';
import {ApplicationFormService} from '../../../../../../../shared/service/form/application-form.service';
import {LayoutService} from "../../../../../../../shared/service/theme/app-layout.service";
import {NewFirewallEntry} from "../../../../../../../shared/model/firewall-entry.model";

@Component({
  selector: 'sm-firewall-entries',
  templateUrl: './firewall-entries.component.html',
  styleUrls: ['./firewall-entries.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    ReactiveFormsModule,
    InputTextModule,
    ButtonModule,
    AccordionModule
  ]
})
export class FirewallEntriesComponent implements OnInit, OnDestroy {
  @Input() containerForm!: FormGroup;
  @Output() entriesUpdated = new EventEmitter<NewFirewallEntry[]>();

  private destroy$ = new Subject<void>();

  constructor(
    public layoutService: LayoutService,
    private applicationFormService: ApplicationFormService) {}

  get firewallEntries(): FormArray {
    return this.containerForm.get('firewallEntries') as FormArray;
  }

  addFirewallEntry(): void {
    const firewallEntriesArray = this.containerForm.get('firewallEntries') as FormArray;
    firewallEntriesArray.push(this.applicationFormService.createFirewallEntryForm());
    this.emitUpdatedFirewallEntries();
  }

  removeFirewallEntry(index: number): void {
    const firewallEntriesArray = this.containerForm.get('firewallEntries') as FormArray;
    firewallEntriesArray.removeAt(index);
    this.emitUpdatedFirewallEntries();
  }

  private emitUpdatedFirewallEntries(): void {
    const firewallEntriesArray = this.containerForm.get('firewallEntries') as FormArray;
    const firewallEntries = firewallEntriesArray.controls.map(control => ({
      id: null,
      cidr: control.get('cidr')?.value,
      level: control.get('level')?.value,
      policy: control.get('policy')?.value,
      rule: control.get('rule')?.value
    }));

    this.entriesUpdated.emit(firewallEntries);
  }

  ngOnInit(): void {
    if (!this.containerForm.contains('firewallEntries')) {
      this.containerForm.addControl('firewallEntries', new FormArray([]));
    }

    const firewallEntriesArray = this.containerForm.get('firewallEntries') as FormArray;
    const existingFirewallEntries = this.containerForm.get('firewallEntries')?.value || [];

    // Clear existing values
    while (firewallEntriesArray.length) {
      firewallEntriesArray.removeAt(0);
    }

    // Add existing or initialize empty
    if (existingFirewallEntries.length) {
      existingFirewallEntries.forEach((firewallEntry: NewFirewallEntry) => {
        firewallEntriesArray.push(this.applicationFormService.createFirewallEntryForm(firewallEntry));
      });
    }

    // Subscribe to changes
    firewallEntriesArray.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.emitUpdatedFirewallEntries();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
