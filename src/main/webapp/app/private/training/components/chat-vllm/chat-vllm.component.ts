import {
  ChangeDetectionStrategy,
  Component,
  effect,
  ElementRef,
  inject,
  input,
  OnDestroy,
  signal,
  ViewChild
} from '@angular/core';
import {PageLoadComponent} from "../../../../shared/components/page-load/page-load.component";
import {AdvancedParam, AttachedFile, ChatMessage, ChatVllmService} from "../../../../shared/service/chat-vllm.service";
import {FormsModule} from "@angular/forms";
import {NgClass, NgForOf, NgIf} from "@angular/common";
import {ButtonDirective} from "primeng/button";
import {ScrollPanelModule} from "primeng/scrollpanel";
import {CardModule} from "primeng/card";
import {SidebarModule} from "primeng/sidebar";
import {AccordionModule} from "primeng/accordion";
import SharedModule from "../../../../shared/shared.module";
import {MarkdownComponent} from "ngx-markdown";
import {TooltipModule} from 'primeng/tooltip';
import {MessageModule} from "primeng/message";
import {InputSwitchModule} from 'primeng/inputswitch';
import {Subscription} from "rxjs";

@Component({
  selector: 'sm-chat-vllm',
  standalone: true,
  imports: [
    PageLoadComponent,
    FormsModule,
    NgForOf,
    NgIf,
    ButtonDirective,
    NgClass,
    ScrollPanelModule,
    CardModule,
    SidebarModule,
    AccordionModule,
    SharedModule,
    MarkdownComponent,
    TooltipModule,
    MessageModule,
    InputSwitchModule
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './chat-vllm.component.html',
  styleUrl: './chat-vllm.component.scss'
})
export class ChatVllmComponent implements OnDestroy {

  readonly chatService = inject(ChatVllmService);

  modelName = input<string>();
  baseUrl = input<string>();
  enableThinkingInput = input<boolean>();
  advancedParamsInput = input<AdvancedParam[]>();
  supportsFiles = input.required<boolean>();
  acceptedFileTypes = input<('image' | 'video' | 'audio')[]>();
  maxFiles = input<number>();
  maxFileSize = input<number>();

  messages = signal<ChatMessage[]>([]);
  live = signal('');
  liveThinking = signal('');
  info = signal<any>(null);
  streaming = signal(false);
  errorMessage = signal<string | null>(null);
  enableThinkingSignal = signal(false);
  attachedFiles = signal<AttachedFile[]>([]);

  @ViewChild('scrollPanel') scrollPanel: any;
  @ViewChild('messageInput') messageInput!: ElementRef;

  draft = '';
  controlsVisible = false;
  systemPrompt = 'You are a helpful assistant.';
  advancedParams: AdvancedParam[] = [];

  private streamSub: Subscription | undefined;
  private scrollTimeout: any;

  constructor() {
    effect(() => {
      const inputValue = this.advancedParamsInput();

      if (!inputValue || inputValue.length === 0) {
        this.advancedParams = [
          {label: 'Temperature', key: 'temperature', type: 'number', value: null, isCustom: false},
          {label: 'Top-p', key: 'top_p', type: 'number', value: null, isCustom: false},
          {label: 'Top-k', key: 'top_k', type: 'number', value: null, isCustom: false},
          {label: 'Max Tokens', key: 'max_tokens', type: 'number', value: null, isCustom: false},
        ];
      } else {
        this.advancedParams = [...inputValue];
      }
    });
  }

  private getEnableThinking(): boolean {
    const inputValue = this.enableThinkingInput();
    return inputValue !== undefined ? inputValue : this.enableThinkingSignal();
  }

  get enableThinking() {
    return this.getEnableThinking();
  }

  set enableThinking(value: boolean) {
    this.enableThinkingSignal.set(value);
  }

  async onFilesSelected(event: Event) {
    const target = event.target as HTMLInputElement;
    const files = target.files;

    if (!files || files.length === 0) return;

    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      await this.processFile(file);
    }

    // Clear the input
    target.value = '';
  }

  private async processFile(file: File) {
    // ✅ Validate file type
    const fileType = this.getFileType(file);
    const acceptedTypes = this.acceptedFileTypes();

    if (!acceptedTypes.includes(fileType)) {
      this.errorMessage.set(
        `File type "${fileType}" not accepted. Accepted types: ${acceptedTypes.join(', ')}`
      );
      return;
    }

    // ✅ Validate max files
    const maxFiles = this.maxFiles();
    if (maxFiles && this.attachedFiles().length >= maxFiles) {
      this.errorMessage.set(`Maximum ${maxFiles} files allowed`);
      return;
    }

    // ✅ Validate file size
    const maxSize = this.maxFileSize();
    if (maxSize && file.size > maxSize * 1024 * 1024) {
      this.errorMessage.set(
        `File "${file.name}" is too large. Maximum size: ${maxSize}MB`
      );
      return;
    }

    // Process file...
    try {
      const attachedFile: AttachedFile = {
        name: file.name,
        size: file.size,
        type: fileType,
        base64: await this.fileToBase64(file),
        preview: '',
        mimeType: file.type,
        url: ''
      };

      if (attachedFile.type === 'image') {
        attachedFile.preview = await this.createImagePreview(file);
        attachedFile.url = attachedFile.preview;
      }

      this.attachedFiles.update(files => [...files, attachedFile]);
    } catch (error) {
      console.error('Error processing file:', error);
      this.errorMessage.set(`Error processing file: ${file.name}`);
    }
  }

  private getFileType(file: File): 'image' | 'audio' | 'video' {
    if (file.type.startsWith('image/')) return 'image';
    if (file.type.startsWith('audio/')) return 'audio';
    if (file.type.startsWith('video/')) return 'video';
    return 'image'; // fallback
  }

  private fileToBase64(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        const result = reader.result as string;
        const base64 = result.split(',')[1];
        resolve(base64);
      };
      reader.onerror = reject;
      reader.readAsDataURL(file);
    });
  }

  private createImagePreview(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = reject;
      reader.readAsDataURL(file);
    });
  }

  removeFile(index: number) {
    this.attachedFiles.update(files => files.filter((_, i) => i !== index));
  }

  async send() {
    if (this.streaming()) {
      this.stop();
      return;
    }

    const userText = this.draft.trim();
    const files = this.attachedFiles();

    if (!userText && files.length === 0) return;

    this.messages.update(ms => [...ms, {
      role: 'user',
      content: userText || 'Attached files',
      files: files.length > 0 ? files : undefined
    }]);

    this.draft = '';
    this.attachedFiles.set([]);
    this.live.set('');
    this.liveThinking.set('');
    this.info.set(null);
    this.streaming.set(true);
    this.errorMessage.set(null);

    setTimeout(() => {
      if (this.messageInput?.nativeElement) {
        const textarea = this.messageInput.nativeElement;
        textarea.style.height = 'auto';
        textarea.rows = 1;
        textarea.dispatchEvent(new Event('input'));
      }
    }, 0);

    this.scrollToBottom();

    const baseUrl = this.baseUrl() || 'http://172.17.30.157:8000/v1';
    const model = this.modelName() || 'Qwen/Qwen2-VL-7B-Instruct-AWQ';
    const thinking = this.getEnableThinking();

    this.streamSub = this.chatService.streamChat$(
      baseUrl,
      model,
      this.messages(),
      this.advancedParams,
      this.systemPrompt,
      thinking
    ).subscribe({
      next: (data) => {
        if (data.isThinking) {
          this.liveThinking.update(v => v + (data.thinking || ''));
        } else {
          this.live.update(v => v + data.chunk);
        }

        this.scrollToBottom();

        if (data.info) {
          this.info.set(data.info);
        }
      },
      complete: () => {
        let finalContent = this.live();
        let finalThinking = this.liveThinking();

        // Check if think tags are still in the content
        if (!finalThinking && finalContent.includes('<think>')) {
          const thinkMatch = finalContent.match(/<think>([\s\S]*?)<\/think>/i);
          if (thinkMatch) {
            finalThinking = thinkMatch[1].trim();
            finalContent = finalContent.replace(/<think>[\s\S]*?<\/think>/i, '').trim();
          }
        }

        if (finalContent || finalThinking) {
          this.messages.update(ms => [
            ...ms,
            {
              role: 'assistant',
              content: finalContent,
              thinking: finalThinking || undefined,
              thinkingExpanded: false,
              info: this.info()
            }
          ]);
        }

        this.live.set('');
        this.liveThinking.set('');
        this.info.set(null);
        this.streaming.set(false);
        this.scrollToBottom();
      },
      error: (err) => {
        console.error('Error in component subscription:', err);
        this.streaming.set(false);
        this.live.set('');
        this.liveThinking.set('');

        let errorMsg = err.message || 'An error occurred while processing your request.';
        this.errorMessage.set(errorMsg);

        setTimeout(() => {
          this.errorMessage.set(null);
        }, 5000);
      }
    });
  }

  stop() {
    if (this.streamSub) {
      this.streamSub.unsubscribe();
      this.streamSub = undefined;
    }

    if (this.live()) {
      this.messages.update(ms => [
        ...ms,
        {
          role: 'assistant',
          content: this.live() + ' [Interrupted]',
          thinking: this.liveThinking() || undefined,
          thinkingExpanded: false,
          info: this.info()
        }
      ]);
    }

    this.streaming.set(false);
    this.live.set('');
    this.liveThinking.set('');
    this.info.set(null);
  }

  toggleThinking(msg: ChatMessage) {
    if (msg.role === 'assistant') {
      msg.thinkingExpanded = !msg.thinkingExpanded;
    }
  }

  private scrollToBottom() {
    if (this.scrollTimeout) {
      clearTimeout(this.scrollTimeout);
    }

    this.scrollTimeout = setTimeout(() => {
      if (this.scrollPanel) {
        const el = this.scrollPanel.contentViewChild?.nativeElement;
        this.scrollPanel.scrollTop(el.scrollHeight);
      }
    }, 10);
  }

  copy(text: string) {
    navigator.clipboard.writeText(text);
  }

  speak(text: string) {
    const utter = new SpeechSynthesisUtterance(text);
    speechSynthesis.speak(utter);
  }

  onEdit(msg: any) {
    console.log("edit", msg);
  }

  getTooltipContent(message: any): string {
    let content = ``;

    if (message.info) {
      content += `Model: ${message.info.model || 'N/A'}\n`;
      content += `Finish: ${message.info.finishReason || 'N/A'}\n\n`;
      content += `Tokens:\n`;
      content += `  Input: ${message.info.usage?.inputTokens || 0}\n`;
      content += `  Output: ${message.info.usage?.outputTokens || 0}\n`;
      content += `  Total: ${message.info.usage?.totalTokens || 0}`;
    }

    return content || 'No info available';
  }

  like(msg: any) {
    console.log("liked");
  }

  dislike(msg: any) {
    console.log("disliked");
  }

  regenerate(msg: any) {
    console.log("regenerate", msg);
  }

  toggleCustom(item: any) {
    item.isCustom = !item.isCustom;

    if (!item.isCustom) {
      item.value = null;
    }
  }

  onValueChange(item: any) {
    if (item.type === 'number' && (item.value === null || item.value === '')) {
      item.isCustom = false;
    }

    if (item.type === 'string' && (!item.value || item.value.trim() === '')) {
      item.isCustom = false;
    }

    if (item.type === 'list' && (!item.value || item.value.trim() === '')) {
      item.isCustom = false;
    }
  }

  onEnter(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      event.stopPropagation();

      const textarea = event.target as HTMLTextAreaElement;
      this.send();

      setTimeout(() => {
        textarea.style.height = 'auto';
        textarea.rows = 1;
        textarea.scrollTop = 0;
      }, 0);
    }
  }

  dismissError() {
    this.errorMessage.set(null);
  }

  ngOnDestroy() {
    if (this.streamSub) {
      this.streamSub.unsubscribe();
    }
    this.streaming.set(false);
  }

  clearError() {
    this.errorMessage.set(null);
  }
}
