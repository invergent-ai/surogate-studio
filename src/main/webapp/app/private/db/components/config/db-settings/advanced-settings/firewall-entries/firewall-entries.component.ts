import {FormArray, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {takeUntil} from 'rxjs/operators';
import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {CommonModule} from '@angular/common';
import {SharedModule} from 'primeng/api';
import {InputTextModule} from 'primeng/inputtext';
import {ButtonModule} from 'primeng/button';
import {AccordionModule} from 'primeng/accordion';
import {Subject} from 'rxjs';
import {NewFirewallEntry} from "../../../../../../../shared/model/firewall-entry.model";
import {DatabaseFormService} from "../../../../../../../shared/service/form/database-form.service";
import {LayoutService} from "../../../../../../../shared/service/theme/app-layout.service";

@Component({
  selector: 'sm-db-firewall-entries',
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
  @Input() databaseForm!: FormGroup;
  @Output() entriesUpdated = new EventEmitter<NewFirewallEntry[]>();

  private destroy$ = new Subject<void>();

  constructor(
    public layoutService: LayoutService,
    private databaseFormService: DatabaseFormService) {}

  get firewallEntries(): FormArray {
    return this.databaseForm.get('firewallEntries') as FormArray;
  }

  addFirewallEntry(): void {
    const entries = this.databaseForm.get('firewallEntries') as FormArray;
    entries.push(this.databaseFormService.createFirewallEntryForm());
    this.emitUpdatedEntries();
  }

  removeFirewallEntry(index: number): void {
    const entries = this.databaseForm.get('firewallEntries') as FormArray;
    entries.removeAt(index);
    this.emitUpdatedEntries();
  }

  private emitUpdatedEntries(): void {
    const entriesArray = this.databaseForm.get('firewallEntries') as FormArray;
    const entries = entriesArray.controls.map(control => ({
      id: null,
      cidr: control.get('cidr')?.value,
      level: control.get('level')?.value,
      policy: control.get('policy')?.value,
      rule: control.get('rule')?.value
    }));

    this.entriesUpdated.emit(entries);
  }

  ngOnInit(): void {
    if (!this.databaseForm.contains('firewallEntries')) {
      this.databaseForm.addControl('firewallEntries', new FormArray([]));
    }

    const entriesArray = this.databaseForm.get('firewallEntries') as FormArray;
    const existingEntries = this.databaseForm.get('firewallEntries')?.value || [];

    // Clear existing values
    while (entriesArray.length) {
      entriesArray.removeAt(0);
    }

    // Add existing or initialize empty
    if (existingEntries.length) {
      existingEntries.forEach((entry: NewFirewallEntry) => {
        entriesArray.push(this.databaseFormService.createFirewallEntryForm(entry));
      });
    }

    entriesArray.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.emitUpdatedEntries();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
