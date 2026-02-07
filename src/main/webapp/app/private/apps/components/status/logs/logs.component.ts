import {
  AfterViewInit,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import {BehaviorSubject, debounceTime, distinctUntilChanged, lastValueFrom, Subject, Subscription} from 'rxjs';
import {takeUntil} from 'rxjs/operators';
import {CommonModule, DatePipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ButtonModule} from 'primeng/button';
import {InputSwitchModule} from 'primeng/inputswitch';
import {InputTextModule} from 'primeng/inputtext';
import {Table, TableModule} from 'primeng/table';
import {TooltipModule} from 'primeng/tooltip';
import {MessageService, SharedModule} from 'primeng/api';
import {PaginatorModule} from 'primeng/paginator';
import {CalendarModule} from 'primeng/calendar';
import {ILog, ILogCriteria, TimeRange, TimeRangeOption} from '../../../../../shared/model/k8s/log.model';
import {LogService} from '../../../../../shared/service/k8s/log.service';
import {SseEvent, SseEventTypeTimeout} from '../../../../../shared/model/k8s/event.model';
import {HighlightModule} from "ngx-highlightjs";
import StripAnsiPipe from '../../../../../shared/pipe/strip-ansi.pipe';

@Component({
  selector: 'sm-logs',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    InputSwitchModule,
    InputTextModule,
    TableModule,
    TooltipModule,
    SharedModule,
    PaginatorModule,
    DatePipe,
    CalendarModule,
    HighlightModule,
    StripAnsiPipe,
  ],
  templateUrl: './logs.component.html',
  styleUrls: ['./logs.component.scss'],
})
export class LogsComponent implements OnInit, OnDestroy, OnChanges, AfterViewInit {
  @Input() resourceId!: string;
  @Input() resourceType!: 'application' | 'taskRun' | 'rayJob';
  @Input() podName!: string;
  @Input() containerId?: string;
  @ViewChild('logTable', { static: false }) logTable?: Table;
  @Output() error = new EventEmitter<any>();
  @Output() logsTimeout = new EventEmitter<any>();
  @Output() logsDisconnect = new EventEmitter<any>();

  // Date filters
  startDate?: Date;
  endDate?: Date;
  logs: ILog[] = [];
  loading = false;
  paused = false;
  private bufferedLogs: ILog[] = [];
  private originalLogs: ILog[] = [];

  private originalBufferedLogs: ILog[] = [];

  // Auto-scroll control
  autoScroll = true;
  timestampColumn: boolean;
  private scrollTimeout?: any;

  // Log limits
  maxLogEntriesInput: string = '100';
  maxLogEntries = 100;
  minLogEntries = 10;
  maxAllowedLogs = 10000;

  // Search
  private searchSubject = new BehaviorSubject<string>('');

  // Time range options
  sinceSeconds?: number;
  timeRangeOptions: TimeRangeOption[] = [
    { label: 'No time filter', value: 'none', seconds: undefined },
    { label: 'Last 5 minutes', value: '5m', seconds: 5 * 60 },
    { label: 'Last 15 minutes', value: '15m', seconds: 15 * 60 },
    { label: 'Last 1 hour', value: '1h', seconds: 60 * 60 },
    { label: 'Last 6 hours', value: '6h', seconds: 6 * 60 * 60 },
  ];
  selectedTimeRange?: TimeRange;

  // Criteria
  criteria: ILogCriteria = {
    applicationId: '',
    podName: '',
    limit: 100,
  };

  private logSubscription?: Subscription;
  private destroy$ = new Subject<void>();
  private initialScrollDone = false;

  constructor(
    private logService: LogService,
    private messageService: MessageService,
  ) {}

  async ngOnInit() {
    await this.initializeComponent();
    this.setupSearchDebounce();
    this.setupScrollListener();
  }

  private setupSearchDebounce(): void {
    this.searchSubject.pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$)).subscribe(() => {
      this.filterClientLogs();
    });
  }

  ngAfterViewInit(): void {
    if (this.logs.length > 0) {
      this.attemptInitialScroll();
    }
  }

  private setupScrollListener(): void {
    if (this.logTable) {
      const viewport = this.logTable.el.nativeElement.querySelector('.p-datatable-wrapper');
      if (viewport) {
        viewport.addEventListener('scroll', () => {
          if (this.paused) {
            if (this.scrollTimeout) {
              clearTimeout(this.scrollTimeout);
            }

            // Only disable auto-scroll temporarily while paused
            this.autoScroll = viewport.scrollHeight - viewport.scrollTop - viewport.clientHeight < 50;

            this.scrollTimeout = setTimeout(() => {
              // Re-enable auto-scroll if not paused
              if (!this.paused) {
                this.autoScroll = true;
              }
            }, 150);
          }
        });
      }
    }
  }

  private async initializeComponent() {
    this.autoScroll = !this.paused;
    this.maxLogEntries = parseInt(this.maxLogEntriesInput, 10);
    const criteria: ILogCriteria = {
      applicationId: this.resourceId,
      podName: this.podName,
      containerId: this.containerId,
      limit: this.maxLogEntries,
      startDate: this.startDate,
      endDate: this.endDate,
      sinceSeconds: this.startDate ? Math.floor((new Date().getTime() - this.startDate.getTime()) / 1000) : undefined,
    };

    this.loading = true;
    this.fetchLogs(criteria);
  }

  get bufferedMessageCount(): number {
    return this.bufferedLogs.length;
  }

  private sortAndFilterLogs(logs: ILog[]): ILog[] {
    const sortedLogs = this.sortLogs(logs);
    const filteredLogs = this.filterLogs(sortedLogs);
    return filteredLogs.slice(-this.maxLogEntries);
  }

  private sortLogs(logs: ILog[]): ILog[] {
    return [...logs].sort((a, b) => {
      const timeA = new Date(a.timestamp).getTime();
      const timeB = new Date(b.timestamp).getTime();
      return timeA - timeB;
    });
  }

  fetchLogsFromTimeAgo(range: TimeRange): void {
    this.sinceSeconds = undefined;
    this.selectedTimeRange = 'none';

    if (range !== 'none') {
      const option = this.timeRangeOptions.find(opt => opt.value === range);
      if (option && option.seconds !== undefined) {
        this.sinceSeconds = option.seconds;
        this.selectedTimeRange = range;
      }
    }

    // Server-side query with time range
    const criteria: ILogCriteria = {
      applicationId: this.resourceId,
      podName: this.podName,
      containerId: this.containerId,
      limit: this.maxLogEntries,
      sinceSeconds: this.sinceSeconds,
      startDate: undefined, // Don't include client-side filters in server query
      endDate: undefined, // Don't include client-side filters in server query
      searchTerm: this.criteria.searchTerm,
    };

    this.fetchLogs(criteria);
  }

  private fetchLogs(criteria: ILogCriteria): void {
    this.loading = true;

    if (this.logSubscription) {
      this.logSubscription.unsubscribe();
      this.logSubscription = null;
    }

    this.logSubscription = this.logService
      .connectToLogStream(this.resourceId, this.resourceType, criteria)
      .pipe(distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe({
        next: (event: SseEvent<ILog[]>) => {
          if (event.type === SseEventTypeTimeout) {
            this.handleTimeout();
            return;
          } else if (event.type === 'complete') {
            this.loading = false;
          } else if (event.type === 'logs') {
            this.loading = false;

            const logsArray = Array.isArray(event.data) ? event.data : [event.data];
            this.logs = this.sortAndFilterLogs(logsArray);

            if (logsArray.length === 1 && logsArray[0]?.type) {
              if (logsArray[0].type === 'timeout') {
                this.logsTimeout.emit();
              } else if (logsArray[0].type === 'disconnect') {
                this.logsDisconnect.emit(logsArray[0].error);
              }
              return;
            }

            if (this.paused) {
              // If paused, accumulate unique logs in buffer
              const uniqueNewLogs = this.filterDuplicateLogs(logsArray, this.bufferedLogs);
              if (uniqueNewLogs.length > 0) {
                this.bufferedLogs = [...this.bufferedLogs, ...uniqueNewLogs];
              }
            } else {
              this.processNewLogs(logsArray);
            }
          }
        },
        error: error => this.handleError(error),
      });
  }

  private attemptInitialScroll(): void {
    if (!this.initialScrollDone && this.logTable?.el?.nativeElement) {
      // Using a larger timeout for initial scroll to ensure table is fully rendered
      setTimeout(() => {
        const viewport = this.logTable?.el.nativeElement.querySelector('.p-datatable-wrapper');
        if (viewport) {
          viewport.scrollTop = viewport.scrollHeight;
          // Try one more time after a longer delay
          setTimeout(() => {
            viewport.scrollTop = viewport.scrollHeight;
            this.initialScrollDone = true;
          }, 200);
        }
      }, 300);
    }
  }

  validateLogLimit(): void {
    const value = parseInt(this.maxLogEntriesInput, 10);

    if (isNaN(value)) {
      this.maxLogEntriesInput = this.maxLogEntries.toString();
      return;
    }

    const oldLimit = this.maxLogEntries;
    const newLimit = Math.max(this.minLogEntries, Math.min(value, this.maxAllowedLogs));

    this.maxLogEntries = newLimit;
    this.maxLogEntriesInput = newLimit.toString();
    this.criteria.limit = newLimit;

    if (newLimit > oldLimit) {
      const additionalLogsNeeded = newLimit - this.originalLogs.length;
      if (additionalLogsNeeded > 0) {
        this.criteria.limit = additionalLogsNeeded;
        this.fetchLogs({
          applicationId: this.resourceId,
          podName: this.podName,
          containerId: this.containerId,
          limit: newLimit,
          sinceSeconds: this.sinceSeconds,
          startDate: this.startDate,
          endDate: this.endDate,
          searchTerm: this.criteria.searchTerm,
        });
      }
    } else {
      this.logs = this.sortAndFilterLogs(this.originalLogs);
      this.bufferedLogs = this.sortAndFilterLogs(this.originalBufferedLogs);
    }
  }

  filterClientLogs(): void {
    if (this.originalLogs.length > 0) {
      this.logs = this.sortAndFilterLogs(this.originalLogs);
    }

    // Force scroll to bottom after filtering
    if (this.autoScroll) {
      this.scrollToBottom();
    }
  }

  private filterDuplicateLogs(newLogs: ILog[], existingLogs: ILog[]): ILog[] {
    const existingKeys = new Set(existingLogs.map(log => `${log.timestamp}-${log.message}`));
    return newLogs.filter(log => {
      const key = `${log.timestamp}-${log.message}`;
      return !existingKeys.has(key);
    });
  }

  private processNewLogs(logs: ILog[]): void {
    // Merge new logs with existing logs
    this.originalLogs = this.mergeLogsUnique([...this.originalLogs, ...logs]);
    // Update displayed logs
    this.logs = this.sortAndFilterLogs(this.originalLogs);
    // Always scroll to bottom when not paused
    if (!this.paused) {
      this.scrollToBottom();
    }
  }

  private scrollToBottom(): void {
    if (!this.logTable?.el?.nativeElement) {
      return;
    }

    requestAnimationFrame(() => {
      const viewport = this.logTable?.el.nativeElement.querySelector('.p-datatable-wrapper');
      if (viewport) {
        const scrollToBottom = () => {
          viewport.scrollTop = viewport.scrollHeight;
        };

        // Immediate scroll attempt
        scrollToBottom();
        // Second attempt after a short delay
        setTimeout(scrollToBottom, 50);
        // Final attempt after a longer delay
        setTimeout(scrollToBottom, 150);
      }
    });
  }

  private filterLogs(logs: ILog[]): ILog[] {
    return logs.filter(log => {
      let matches = true;

      // Client-side search filter
      if (this.criteria.searchTerm?.trim()) {
        matches = matches && log.message.toLowerCase().includes(this.criteria.searchTerm.toLowerCase().trim());
      }

      // Client-side date filter
      if (matches && (this.startDate || this.endDate)) {
        const logDate = new Date(log.timestamp);
        if (this.startDate) {
          matches = matches && logDate >= this.startDate;
        }
        if (this.endDate) {
          matches = matches && logDate <= this.endDate;
        }
      }

      return matches;
    });
  }

  private mergeLogsUnique(logs: ILog[]): ILog[] {
    const uniqueLogs: ILog[] = [];
    const seen = new Set<string>();

    for (const log of logs) {
      const key = `${log.timestamp}-${log.message}`;
      if (!seen.has(key)) {
        seen.add(key);
        uniqueLogs.push(log);
      }
    }

    return uniqueLogs;
  }

  onCalendarModelChange(type: 'start' | 'end'): void {
    // If the model becomes null, it means the calendar was cleared
    if (type === 'start' && this.startDate === null) {
      this.startDate = undefined;
      this.endDate = undefined;
      this.filterClientLogs();
    } else if (type === 'end' && this.endDate === null) {
      this.endDate = undefined;
      this.filterClientLogs();
    }
  }

  onDateFilterChange(): void {
    this.filterClientLogs(); // Only filter existing logs
  }

  public refreshConnection(): void {
    this.autoScroll = true;

    // Clear existing subscription
    if (this.logSubscription) {
      this.logSubscription.unsubscribe();
      this.logSubscription = null;
    }

    // Reset state
    this.loading = true;
    this.paused = false;
    this.bufferedLogs = [];
    this.originalBufferedLogs = [];

    // Reinitialize the component
    this.initializeComponent();

    // Show success message
    this.messageService.add({
      severity: 'success',
      summary: 'Connection Restored',
      detail: 'The connection has been reestablished.',
      life: 3000,
    });
  }

  onDateFilterClear(type: 'start' | 'end'): void {
    if (type === 'start') {
      this.startDate = undefined;
      // If we clear start date, also clear end date as it depends on start
      this.endDate = undefined;
    } else {
      this.endDate = undefined;
    }
    // Force re-filtering of existing logs
    this.filterClientLogs();

    // Force change detection
    this.logs = [...this.logs];
  }

  handleTimeout() {
    console.log('timeout');
  }

  private handleError(error: any): void {
    this.error.emit(error);
    this.messageService.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'An error occurred',
    });
    this.loading = false;
  }

  togglePause(): void {
    this.paused = !this.paused;
    // When unpausing, enable auto-scroll and scroll to bottom
    if (!this.paused) {
      this.autoScroll = true;
      this.scrollToBottom();
    }
    if (!this.paused) {
      if (this.bufferedLogs.length > 0) {
        // Filter out any duplicates before merging
        const uniqueBufferedLogs = this.filterDuplicateLogs(this.bufferedLogs, this.originalLogs);
        if (uniqueBufferedLogs.length > 0) {
          this.originalLogs = this.mergeLogsUnique([...this.originalLogs, ...uniqueBufferedLogs]);
          this.logs = this.sortAndFilterLogs(this.originalLogs);
        }

        // Clear buffer
        this.bufferedLogs = [];
      }
    }
  }

  onSearchChange(searchTerm: string): void {
    this.searchSubject.next(searchTerm);
  }

  downloadLogs(): void {
    const logsToDownload = this.paused ? [...this.originalBufferedLogs, ...this.originalLogs] : this.originalLogs;

    const sortedLogs = this.sortLogs(logsToDownload);
    const csvContent = this.convertLogsToCSV(sortedLogs);
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `application-logs-${new Date().toISOString()}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }

  private convertLogsToCSV(logs: ILog[]): string {
    const headers = ['Timestamp', 'Message'];
    const rows = logs.map(log => [new Date(log.timestamp).toISOString(), `"${log.message.replace(/"/g, '""')}"`]);

    return [headers.join(','), ...rows.map(row => row.join(','))].join('\n');
  }

  clearLogs(): void {
    this.originalLogs = [];
    this.bufferedLogs = [];
    this.logs = [];
  }

  async ngOnChanges(changes: SimpleChanges) {
    if (changes['containerId'] && !changes['containerId'].firstChange) {
      await this.reInit();
    }
  }

  public async reInit() {
    if (this.logSubscription) {
      this.logSubscription.unsubscribe();
      this.logSubscription = null;
    }
    await this.initializeComponent();
  }

  async ngOnDestroy() {
    if (this.logSubscription) {
      this.logSubscription.unsubscribe();
    }
    if (this.scrollTimeout) {
      clearTimeout(this.scrollTimeout);
    }
    this.destroy$.next();
    this.destroy$.complete();
    this.searchSubject.complete();

    await lastValueFrom(this.logService.stopLogs(this.resourceId, this.resourceType, this.podName, this.containerId));
  }

  protected readonly String = String;
}
