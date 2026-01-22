import {
  AfterViewInit,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import {MessageService} from 'primeng/api';
import {NgTerminal, NgTerminalModule} from "ng-terminal";
import {takeUntil} from "rxjs/operators";
import {lastValueFrom, Subject, Subscription} from "rxjs";
import {CommonModule} from "@angular/common";
import {TooltipModule} from "primeng/tooltip";
import {TerminalService} from "../../../../../shared/service/k8s/terminal.service";

@Component({
  selector: 'sm-terminal',
  standalone: true,
  imports: [
    CommonModule,
    NgTerminalModule,
    TooltipModule
  ],
  templateUrl: './terminal.component.html',
  styleUrls: ['./terminal.component.scss'],
})
export class TerminalComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() applicationId!: string;
  @Input() podName!: string;
  @Input() vmId!: string;
  @Input() containerId?: string;
  @Output() error = new EventEmitter<any>();
  @Output() terminalTimeout = new EventEmitter<any>();
  @Output() terminalDisconnect = new EventEmitter<any>();
  @ViewChild('term', { static: false }) child!: NgTerminal;

  theme = {
    foreground: '#F8F8F8',
    background: '#2D2E2C',
    selectionBackground: '#5DA5D533',
    black: '#1E1E1D',
    brightBlack: '#262625',
    red: '#CE5C5C',
    brightRed: '#FF7272',
    green: '#5BCC5B',
    brightGreen: '#72FF72',
    yellow: '#CCCC5B',
    brightYellow: '#FFFF72',
    blue: '#5D5DD3',
    brightBlue: '#7279FF',
    magenta: '#BC5ED1',
    brightMagenta: '#E572FF',
    cyan: '#5DA5D5',
    brightCyan: '#72F0FF',
    white: '#F8F8F8',
    brightWhite: '#FFFFFF',
    border: '#85858a',
  };

  private termSubscription?: Subscription;
  private childSubscription?: Subscription;
  private destroy$ = new Subject<void>();

  constructor(private terminalService: TerminalService,
              private messageService: MessageService) {}

  ngAfterViewInit() {
    this.init();
  }

  init() {
    const startFunc = () => {
      if (this.applicationId && this.containerId) {
        return this.terminalService.startAppTerminal(this.applicationId, this.podName, this.containerId);
      } else {
        return this.terminalService.startVmTerminal(this.vmId);
      }
    }

    startFunc().subscribe({
      next: () => {
        this.initTerminal();
        this.connectToTerminal();
      },
      error: (error) => console.log(error)
    });

  }

  public refreshConnection(): void {
    this.init();
  }

  initTerminal() {
    this.child.setXtermOptions({
      fontSize: 18,
      cursorBlink: true,
      theme: this.theme
    });
    this.child.underlying.focus();
    if (!this.childSubscription) {
      this.childSubscription = this.child.onData().subscribe(data => {
        this.sendCommand(data);
      });
    }
  }

  connectToTerminal(): void {
    if (this.termSubscription) {
      return;
    }

    const fn = () => {
      if (this.applicationId && this.containerId) {
        return this.terminalService.connectToAppTerminal(this.applicationId, this.podName, this.containerId);
      } else {
        return this.terminalService.connectToVmTerminal(this.vmId);
      }
    };

    this.termSubscription = fn()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (message) => {
          if (message && message.body) {
            const body = JSON.parse(message.body);
            if (body?.type) {
              if (body.type === "timeout") {
                this.terminalTimeout.emit();
              } else if (body.type === "disconnect") {
                this.terminalDisconnect.emit(body.error);
              }
              return;
            }
          }

          this.writeLines(message.binaryBody);
        },
        error: this.handleError.bind(this)
      });
  }

  private writeLines(binaryBody: Uint8Array): void {
    const messageRaw = new TextDecoder().decode(binaryBody);
    if (messageRaw) {
      const messageJson = JSON.parse(messageRaw);
      if (messageJson) {
        let message = messageJson.payload;
        if (message) {
          this.child.write(message);
        }
      }
    }
  }

  private sendCommand(command: string): void {
    if (this.applicationId && this.containerId) {
      this.terminalService.sendAppTerminalCommand(this.applicationId, this.podName, this.containerId, command);
    } else {
      this.terminalService.sendVmTerminalCommand(this.vmId, command);
    }
  }

  private handleError(error: any): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'An error occurred'
    });
  }

  async ngOnChanges(changes: SimpleChanges) {
    if (changes['containerId'] && !changes['containerId'].firstChange) {
      if (this.termSubscription) {
        this.termSubscription.unsubscribe();
        this.termSubscription = null;
      }
      if (this.childSubscription) {
        this.childSubscription.unsubscribe();
        this.childSubscription = null;
      }
      if (this.child?.underlying) {
        this.child.underlying.clear();
      }
      this.init();
    }
  }

  async ngOnDestroy() {
    if (this.termSubscription) {
      this.termSubscription.unsubscribe();
    }
    if (this.childSubscription) {
      this.childSubscription.unsubscribe();
    }

    this.destroy$.next();
    this.destroy$.complete();

    if (this.applicationId && this.containerId) {
      await lastValueFrom(this.terminalService.stopAppTerminal(this.applicationId, this.podName, this.containerId));
    } else {
      await lastValueFrom(this.terminalService.stopVmTerminal(this.vmId));
    }
  }
}
