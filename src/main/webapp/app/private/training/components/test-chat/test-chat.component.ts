import {Component, effect, EventEmitter, input, OnDestroy, Output, signal, ViewChild} from '@angular/core';
import {MessageService} from 'primeng/api';
import {lastValueFrom, Subject, Subscription} from 'rxjs';
import {CommonModule} from '@angular/common';
import {ChartModule} from 'primeng/chart';
import {TooltipModule} from 'primeng/tooltip';
import {TagModule} from 'primeng/tag';
import {DropdownModule} from 'primeng/dropdown';
import {FormsModule} from '@angular/forms';
import {CardModule} from 'primeng/card';
import {IJob} from "../../../../shared/model/job.model";
import {PageLoadComponent} from "../../../../shared/components/page-load/page-load.component";
import {TestChatService} from "../../../../shared/service/k8s/test-chat.service";
import {takeUntil} from "rxjs/operators";
import {ScrollPanel, ScrollPanelModule} from "primeng/scrollpanel";
import {AvatarModule} from "primeng/avatar";
import {ToolbarModule} from "primeng/toolbar";
import {DividerModule} from "primeng/divider";
import {InputTextareaModule} from "primeng/inputtextarea";
import {ButtonDirective} from "primeng/button";
import {Highlight} from "ngx-highlightjs";

type Role = 'system' | 'user';
interface Msg { role: Role; content: string; }

@Component({
  selector: 'sm-test-chat',
  standalone: true,
  imports: [
    CommonModule,
    ChartModule,
    TooltipModule,
    TagModule,
    DropdownModule,
    FormsModule,
    CardModule,
    PageLoadComponent,
    ScrollPanelModule,
    AvatarModule,
    ToolbarModule,
    DividerModule,
    InputTextareaModule,
    ButtonDirective,
    Highlight
  ],
  templateUrl: './test-chat.component.html'
})
export class TestChatComponent implements OnDestroy {
  STREAMING_MODE = true;

  rayJob = input.required<IJob>();
  @ViewChild('scrollPanel') scrollPanel!: ScrollPanel;
  @Output() error = new EventEmitter<any>();

  messages = signal<Msg[]>([{ role: 'system', content: "Let's start testing the model." }]);
  live = signal('');
  streaming = signal(false);
  loading = true;
  errorMessage: string;
  draft = '';

  private subscription?: Subscription;
  private destroy$ = new Subject<void>();

  constructor(private messageService: MessageService,
              private testChatService: TestChatService) {
    effect(async () => {
      if (this.rayJob()) {
        this.startChat();
      }
    });
  }

  private startChat() {
    this.testChatService.startChat(this.rayJob().id).subscribe({
      next: () => {
        this.connectToChat();
        this.loading = false;
      },
      error: (error) =>{
        console.log(error);
        this.errorMessage = 'Chat could not be started.'
        this.loading = false;
      }
    });
  }

  connectToChat(): void {
    if (this.subscription) {
      return;
    }

    this.testChatService.connectToJobChat(this.rayJob().id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (message) => {
          if (message && message.body) {
            const body = JSON.parse(message.body);
            if (body?.error) {
              this.messages.update(ms => [...ms, { role: 'system', content: `⚠️ ${String(body?.error)}` }]);
              this.scrollToBottom();
              this.finish();
              return;
            }
          }
          this.streaming.set(true)
          this.write(message.binaryBody);
        },
        error: this.handleError.bind(this)
      });
  }

  private write(binaryBody: Uint8Array): void {
    const messageRaw = new TextDecoder().decode(binaryBody);
    if (messageRaw) {
      const messageJson = JSON.parse(messageRaw);
      if (messageJson) {
        const message = messageJson.payload;
        if (message) {
          if (this.STREAMING_MODE) {
            if (message === '[DONE]') {
              this.messages.update(ms => [...ms, { role: 'system', content: this.live() }]);
              this.scrollToBottom();
              this.finish();
            } else {
              this.live.set(this.live() + message);
              this.scrollToBottom();
            }
          } else {
            this.messages.update(ms => [...ms, { role: 'system', content: message }]);
            this.scrollToBottom();
            this.finish();
          }
        }
      }
    }
  }

  onComposerKeydown(e: KeyboardEvent) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      if (!this.streaming() && this.draft?.trim()) {
        this.sendMessage();
      }
    }
  }

  sendMessage(): void {
    if (this.streaming()) {
      return;
    }
    const q = this.draft.trim();
    if (!q) {
      return;
    }

    this.messages.update(ms => [...ms, { role: 'user', content: q }]);
    this.live.set('');
    this.streaming.set(true);
    this.testChatService.sendChatMessage(this.rayJob().id, this.draft);

    this.draft = '';
  }

  private finish() {
    this.streaming.set(false);
    this.live.set('');
  }

  private handleError(error: any): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'An error occurred'
    });
  }

  private scrollToBottom(): void {
    if (this.scrollPanel) {
      const contentEl = this.scrollPanel.contentViewChild?.nativeElement;
      if (contentEl) {
        this.scrollPanel.scrollTop(contentEl.scrollHeight);
      }
    }
  }

  async ngOnDestroy() {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }

    this.destroy$.next();
    this.destroy$.complete();

    await lastValueFrom(this.testChatService.stopChat(this.rayJob().id));
  }
}
