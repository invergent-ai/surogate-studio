import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { BehaviorSubject, debounceTime, distinctUntilChanged, lastValueFrom, Subject, Subscription } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';
import { Table, TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { MessageService, SharedModule } from 'primeng/api';
import { PaginatorModule } from 'primeng/paginator';
import { CalendarModule } from 'primeng/calendar';
import { ILog, ILogCriteria, TimeRange, TimeRangeOption } from '../../../../../shared/model/k8s/log.model';
import { LogService } from '../../../../../shared/service/k8s/log.service';
import { SseEvent, SseEventTypeTimeout } from '../../../../../shared/model/k8s/event.model';
import { HighlightModule } from 'ngx-highlightjs';
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
  changeDetection: ChangeDetectionStrategy.OnPush,
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
  private historyTailLines = 0;
  private lastHistoryBatchHash = 0;

  // Auto-scroll control
  autoScroll = true;
  timestampColumn: boolean;

  // Log limits
  maxLogEntriesInput: string = '100';
  maxLogEntries = 100;
  minLogEntries = 10;
  maxAllowedLogs = 10000;
  private readonly HISTORY_PAGE_SIZE = 20;

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

  // History fetch (infinite scroll up)
  private fetchingOlderLogs = false;
  private allOlderLogsLoaded = false;
  private scrollListenerAttached = false;
  browsingHistory = false;
  newLogsWhileBrowsing = 0;
  private consecutiveEmptyFetches = 0;

  private logSubscription?: Subscription;
  private destroy$ = new Subject<void>();
  private initialScrollDone = false;

  constructor(
    private logService: LogService,
    private messageService: MessageService,
    private cdr: ChangeDetectorRef,
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

  private hashString(str: string): number {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = (hash << 5) - hash + char;
      hash |= 0;
    }
    return hash;
  }

  private setupScrollListener(): void {
    const trySetup = () => {
      if (this.scrollListenerAttached) return;
      if (!this.logTable) {
        setTimeout(trySetup, 200);
        return;
      }
      const viewport = this.logTable.el.nativeElement.querySelector('.p-datatable-wrapper');
      if (!viewport) {
        setTimeout(trySetup, 200);
        return;
      }

      this.scrollListenerAttached = true;

      viewport.addEventListener('scroll', () => {
        const distanceFromBottom = viewport.scrollHeight - viewport.scrollTop - viewport.clientHeight;
        const wasAutoScroll = this.autoScroll;
        this.autoScroll = distanceFromBottom < 50;

        if (!this.autoScroll && wasAutoScroll && !this.browsingHistory) {
          this.browsingHistory = true;
          this.newLogsWhileBrowsing = 0;
          this.cdr.markForCheck();
        }

        if (this.autoScroll && this.browsingHistory) {
          this.browsingHistory = false;
          this.newLogsWhileBrowsing = 0;
          this.cdr.markForCheck();
        }

        if (
          viewport.scrollTop < 50 &&
          !this.fetchingOlderLogs &&
          !this.allOlderLogsLoaded &&
          !this.loading &&
          this.originalLogs.length > 0
        ) {
          this.fetchOlderLogs(viewport);
        }
      });

      const unstickInterval = setInterval(() => {
        if (!this.scrollListenerAttached) {
          clearInterval(unstickInterval);
          return;
        }
        if (
          viewport.scrollTop < 5 &&
          !this.fetchingOlderLogs &&
          !this.allOlderLogsLoaded &&
          !this.loading &&
          this.originalLogs.length > 0
        ) {
          this.fetchOlderLogs(viewport);
        }
      }, 500);

      this.destroy$.subscribe(() => clearInterval(unstickInterval));
    };
    trySetup();
  }

  private async initializeComponent() {
    this.autoScroll = !this.paused;
    this.allOlderLogsLoaded = false;
    this.fetchingOlderLogs = false;
    this.browsingHistory = false;
    this.historyTailLines = 0;
    this.consecutiveEmptyFetches = 0;
    this.newLogsWhileBrowsing = 0;
    this.lastHistoryBatchHash = 0;
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
    this.cdr.markForCheck();
    this.fetchLogs(criteria);
  }

  get bufferedMessageCount(): number {
    return this.bufferedLogs.length;
  }

  private sortAndFilterLogs(logs: ILog[]): ILog[] {
    const sortedLogs = this.sortLogs(logs);
    const filteredLogs = this.filterLogs(sortedLogs);
    if (this.browsingHistory) {
      return filteredLogs;
    }
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
    this.allOlderLogsLoaded = false;
    this.fetchingOlderLogs = false;
    this.browsingHistory = false;
    this.newLogsWhileBrowsing = 0;

    if (range !== 'none') {
      const option = this.timeRangeOptions.find(opt => opt.value === range);
      if (option && option.seconds !== undefined) {
        this.sinceSeconds = option.seconds;
        this.selectedTimeRange = range;
      }
    }

    const criteria: ILogCriteria = {
      applicationId: this.resourceId,
      podName: this.podName,
      containerId: this.containerId,
      limit: this.maxLogEntries,
      sinceSeconds: this.sinceSeconds,
      startDate: undefined,
      endDate: undefined,
      searchTerm: this.criteria.searchTerm,
    };

    this.fetchLogs(criteria);
  }

  private fetchLogs(criteria: ILogCriteria): void {
    this.loading = true;
    this.cdr.markForCheck();

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
            this.cdr.markForCheck();
          } else if (event.type === 'logs') {
            this.loading = false;

            const logsArray = Array.isArray(event.data) ? event.data : [event.data];

            if (logsArray.length === 1 && logsArray[0]?.type) {
              if (logsArray[0].type === 'timeout') {
                this.logsTimeout.emit();
              } else if (logsArray[0].type === 'disconnect') {
                this.logsDisconnect.emit(logsArray[0].error);
              }
              this.cdr.markForCheck();
              return;
            }

            if (this.paused) {
              const uniqueNewLogs = this.filterDuplicateLogs(logsArray, this.bufferedLogs);
              if (uniqueNewLogs.length > 0) {
                this.bufferedLogs = [...this.bufferedLogs, ...uniqueNewLogs];
              }
            } else {
              this.processNewLogs(logsArray);
            }
            this.cdr.markForCheck();
          }
        },
        error: error => this.handleError(error),
      });
  }

  private fetchOlderLogs(viewport: HTMLElement): void {
    if (this.fetchingOlderLogs || this.allOlderLogsLoaded || this.originalLogs.length === 0) {
      return;
    }

    this.fetchingOlderLogs = true;

    if (this.historyTailLines === 0) {
      this.historyTailLines = this.originalLogs.length;
    }
    this.historyTailLines += this.HISTORY_PAGE_SIZE * 2;

    const criteria: ILogCriteria = {
      applicationId: this.resourceId,
      podName: this.podName,
      containerId: this.containerId,
      limit: this.HISTORY_PAGE_SIZE,
      tailLines: this.historyTailLines,
    };

    this.logService
      .fetchLogHistory(this.resourceId, this.resourceType, criteria)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (logs: ILog[]) => {
          if (!logs || logs.length === 0) {
            this.allOlderLogsLoaded = true;
            this.fetchingOlderLogs = false;
            this.cdr.markForCheck();
            return;
          }

          const batchHash = this.hashString(logs.map(l => l.message).join('\n'));
          if (batchHash === this.lastHistoryBatchHash) {
            this.consecutiveEmptyFetches++;
            if (this.consecutiveEmptyFetches >= 2) {
              this.allOlderLogsLoaded = true;
              this.fetchingOlderLogs = false;
              this.cdr.markForCheck();
              return;
            }
          } else {
            this.consecutiveEmptyFetches = 0;
          }
          this.lastHistoryBatchHash = batchHash;

          if (logs.length < this.HISTORY_PAGE_SIZE) {
            this.allOlderLogsLoaded = true;
          }

          this.browsingHistory = true;
          this.originalLogs = [...logs, ...this.originalLogs];

          const prevScrollHeight = viewport.scrollHeight;
          const prevScrollTop = viewport.scrollTop;

          this.logs = this.sortLogs(this.filterLogs(this.originalLogs));
          this.cdr.detectChanges();

          requestAnimationFrame(() => {
            requestAnimationFrame(() => {
              const newScrollHeight = viewport.scrollHeight;
              const heightAdded = newScrollHeight - prevScrollHeight;
              const offset = this.allOlderLogsLoaded ? 0 : 200;
              viewport.scrollTop = prevScrollTop + heightAdded + offset;

              setTimeout(() => {
                this.fetchingOlderLogs = false;
                if (viewport.scrollTop < 50 && !this.allOlderLogsLoaded && !this.loading) {
                  this.fetchOlderLogs(viewport);
                }
              }, 300);
            });
          });
        },
        error: () => {
          this.fetchingOlderLogs = false;
        },
      });
  }

  private attemptInitialScroll(): void {
    if (!this.initialScrollDone && this.logTable?.el?.nativeElement) {
      setTimeout(() => {
        const viewport = this.logTable?.el.nativeElement.querySelector('.p-datatable-wrapper');
        if (viewport) {
          viewport.scrollTop = viewport.scrollHeight;
          setTimeout(() => {
            viewport.scrollTop = viewport.scrollHeight;
            this.initialScrollDone = true;
          }, 200);
        }
      }, 300);
    }
  }

  validateLogLimit(): void {
    this.allOlderLogsLoaded = false;
    this.fetchingOlderLogs = false;

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
      this.cdr.markForCheck();
    }
  }

  filterClientLogs(): void {
    if (this.originalLogs.length > 0) {
      this.logs = this.sortAndFilterLogs(this.originalLogs);
    }

    if (this.autoScroll && !this.fetchingOlderLogs && !this.browsingHistory) {
      this.scrollToBottom();
    }
    this.cdr.markForCheck();
  }

  private filterDuplicateLogs(newLogs: ILog[], existingLogs: ILog[]): ILog[] {
    const existingKeys = new Set(existingLogs.map(log => `${log.timestamp}-${this.stripAnsi(log.message)}`));
    return newLogs.filter(log => {
      const key = `${log.timestamp}-${this.stripAnsi(log.message)}`;
      return !existingKeys.has(key);
    });
  }

  private processNewLogs(logs: ILog[]): void {
    const prevCount = this.originalLogs.length;
    this.originalLogs = this.mergeLogsUnique([...this.originalLogs, ...logs]);
    this.logs = this.sortAndFilterLogs(this.originalLogs);

    if (this.autoScroll && !this.paused && !this.browsingHistory) {
      this.scrollToBottom();
    } else if (this.browsingHistory) {
      const newCount = this.originalLogs.length - prevCount;
      if (newCount > 0) {
        this.newLogsWhileBrowsing += newCount;
        this.cdr.detectChanges();
      }
    }
  }

  private scrollToBottom(): void {
    if (!this.logTable?.el?.nativeElement) {
      return;
    }

    requestAnimationFrame(() => {
      const viewport = this.logTable?.el.nativeElement.querySelector('.p-datatable-wrapper');
      if (viewport) {
        viewport.scrollTop = viewport.scrollHeight;
        setTimeout(() => {
          viewport.scrollTop = viewport.scrollHeight;
        }, 100);
      }
    });
  }

  jumpToBottom(): void {
    this.browsingHistory = false;
    this.newLogsWhileBrowsing = 0;
    this.autoScroll = true;
    this.logs = this.sortAndFilterLogs(this.originalLogs);
    this.cdr.markForCheck();
    this.scrollToBottom();
  }

  private filterLogs(logs: ILog[]): ILog[] {
    return logs.filter(log => {
      let matches = true;

      if (this.criteria.searchTerm?.trim()) {
        matches = matches && log.message.toLowerCase().includes(this.criteria.searchTerm.toLowerCase().trim());
      }

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
      const stripped = this.stripAnsi(log.message);
      const key = `${log.timestamp}-${stripped}`;
      if (!seen.has(key)) {
        seen.add(key);
        uniqueLogs.push(log);
      }
    }

    return uniqueLogs;
  }

  private stripAnsi(str: string): string {
    return str.replace(/\x1B\[[0-9;]*[a-zA-Z]|\x1B\].*?\x07|\r/g, '');
  }

  onCalendarModelChange(type: 'start' | 'end'): void {
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
    this.filterClientLogs();
  }

  public refreshConnection(): void {
    this.autoScroll = true;
    this.browsingHistory = false;
    this.newLogsWhileBrowsing = 0;

    if (this.logSubscription) {
      this.logSubscription.unsubscribe();
      this.logSubscription = null;
    }

    this.loading = true;
    this.paused = false;
    this.bufferedLogs = [];
    this.originalBufferedLogs = [];

    this.initializeComponent();

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
      this.endDate = undefined;
    } else {
      this.endDate = undefined;
    }
    this.filterClientLogs();
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
    this.cdr.markForCheck();
  }

  togglePause(): void {
    this.paused = !this.paused;
    if (!this.paused) {
      this.autoScroll = true;
      this.browsingHistory = false;
      this.newLogsWhileBrowsing = 0;
      if (this.bufferedLogs.length > 0) {
        const uniqueBufferedLogs = this.filterDuplicateLogs(this.bufferedLogs, this.originalLogs);
        if (uniqueBufferedLogs.length > 0) {
          this.originalLogs = this.mergeLogsUnique([...this.originalLogs, ...uniqueBufferedLogs]);
          this.logs = this.sortAndFilterLogs(this.originalLogs);
        }
        this.bufferedLogs = [];
      }
      this.scrollToBottom();
    }
    this.cdr.markForCheck();
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
    this.allOlderLogsLoaded = false;
    this.fetchingOlderLogs = false;
    this.browsingHistory = false;
    this.newLogsWhileBrowsing = 0;
    this.cdr.markForCheck();
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
    this.destroy$.next();
    this.destroy$.complete();
    this.searchSubject.complete();

    await lastValueFrom(this.logService.stopLogs(this.resourceId, this.resourceType, this.podName, this.containerId));
  }

  protected readonly String = String;
}
