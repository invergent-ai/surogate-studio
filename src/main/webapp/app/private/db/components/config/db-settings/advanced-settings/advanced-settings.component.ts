import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {InputSwitchModule} from 'primeng/inputswitch';
import {InputTextareaModule} from 'primeng/inputtextarea';
import {NgIf} from '@angular/common';
import SharedModule from '../../../../../../shared/shared.module';
import {FormGroup, ReactiveFormsModule} from '@angular/forms';
import {Subject, takeUntil} from 'rxjs';
import {RadioButtonModule} from 'primeng/radiobutton';
import {ButtonModule} from 'primeng/button';
import {InputTextModule} from 'primeng/inputtext';
import {ResourceSettingsComponent} from "./resource-settings/resource-settings.component";
import {FirewallEntriesComponent} from "./firewall-entries/firewall-entries.component";
import {NewFirewallEntry} from "../../../../../../shared/model/firewall-entry.model";

@Component({
  selector: 'sm-db-advanced-settings',
  standalone: true,
  imports: [
    InputSwitchModule,
    InputTextareaModule,
    NgIf,
    SharedModule,
    ReactiveFormsModule,
    RadioButtonModule,
    ButtonModule,
    InputTextModule,
    ResourceSettingsComponent,
    FirewallEntriesComponent,
    FirewallEntriesComponent
  ],
  templateUrl: './advanced-settings.component.html',
  styleUrls: ['./advanced-settings.component.scss'],
})
export class AdvancedSettingsComponent implements OnInit, OnDestroy {
  @Input() databaseForm!: FormGroup;
  @Input()
  set resetTrigger(input: Subject<void>) {
    if (input) {
      this._resetTrigger = input;
      this._resetTrigger.pipe(
        takeUntil(this.destroy$)
      ).subscribe(() => {
        this.resetComponentState();
      });
    }
  }

  private destroy$ = new Subject<void>();
  _resetTrigger!: Subject<void>;
  private _showForm = false;

  get showForm(): boolean {
    return this._showForm;
  }

  set showForm(value: boolean) {
    this._showForm = value;
  }

  ngOnInit() {}

  onFirewallEntriesUpdated(firewallEntries: NewFirewallEntry[]): void {
    this.databaseForm.patchValue({
      firewallEntries: firewallEntries
    }, { emitEvent: false });

    this.databaseForm.markAsDirty();
  }

  private resetComponentState(): void {
    this._showForm = false;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
