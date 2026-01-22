import {Component, computed, effect, EventEmitter, Input, signal, OnInit, Output, OnChanges} from '@angular/core';
import SharedModule from '../../../../shared/shared.module';
import {TooltipModule} from 'primeng/tooltip';
import {ControlService} from '../../../../shared/service/k8s/control.service';
import {MessageService} from 'primeng/api';
import {ConfirmPopupModule} from 'primeng/confirmpopup';
import {OverlayPanelModule} from 'primeng/overlaypanel';
import {ReactiveFormsModule} from '@angular/forms';
import {finalize} from 'rxjs/operators';
import {IDatabase} from '../../../../shared/model/database.model';
import {DatabaseStatus} from '../../../../shared/model/enum/database-status.model';
import {DbStatus} from '../../../../shared/model/k8s/db-status.model';

@Component({
  standalone: true,
  selector: 'sm-db-control',
  templateUrl: './db-control.component.html',
  imports: [
    SharedModule,
    TooltipModule,
    ConfirmPopupModule,
    OverlayPanelModule,
    ReactiveFormsModule
  ]
})
export class DbControlComponent implements OnChanges {
  @Input() db: IDatabase;
  @Input() dbStatus: DbStatus;
  @Output() controlFinished = new EventEmitter<any>();

  loadingStart = signal(false);
  loadingStop = signal(false);


  private dbStatusSignal = signal<DbStatus | null>(null);

  ngOnChanges() {
    this.dbStatusSignal.set(this.dbStatus);
  }

  // Computed loading state
  loading = computed(() => {
    const status = this.dbStatusSignal() || this.dbStatus;
    const resourcesPending = status?.stage === 'INITIALIZING' ||
      status?.stage === 'WAITING';
    return this.loadingStart() || this.loadingStop() || resourcesPending;
  });

  // Smart button disabling based on status
  startDisabled = computed(() => {
    if (!this.db) return true;

    // Only allow start when database is deployed but not running
    if (this.db.status !== DatabaseStatus.DEPLOYED) {
      return true;
    }

    // Disable if already running or in transition
    const status = this.dbStatusSignal() || this.dbStatus;
    const activeStages = ['RUNNING', 'INITIALIZING', 'WAITING', 'RESTARTING'];
    return status?.stage && activeStages.includes(status.stage);
  });

  stopDisabled = computed(() => {
    if (!this.db) return true;

    // Only allow stop when deployed
    if (this.db.status !== DatabaseStatus.DEPLOYED) {
      return true;
    }

    // Disable if not running
    const status = this.dbStatusSignal() || this.dbStatus;
    return !status?.stage || status.stage !== 'RUNNING';
  });

  constructor(
    private controlService: ControlService,
    private messageService: MessageService
  ) {}

  async startDb() {
    this.loadingStart.set(true);
    this.controlService.startDatabase(this.db?.id)
      .pipe(finalize(() => this.loadingStart.set(false)))
      .subscribe({
        next: (success) => this.handleSuccess(success),
        error: this.handleError.bind(this)
      });
  }

  async stopDb() {
    this.loadingStop.set(true);
    this.controlService.stopDatabase(this.db?.id)
      .pipe(finalize(() => this.loadingStop.set(false)))
      .subscribe({
        next: (success) => this.handleSuccess(success),
        error: this.handleError.bind(this)
      });
  }

  private handleSuccess(success: boolean): void {
    if (success) {
      this.controlFinished.emit();
      return;
    }
    this.handleError(null);
  }

  private handleError(error: any): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Error',
      detail: error?.message || 'An error occurred'
    });
  }
}
