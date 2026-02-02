import {
  ChangeDetectionStrategy,
  Component,
  effect,
  ElementRef,
  inject,
  input,
  OnDestroy, OnInit,
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
import {Subject, Subscription} from "rxjs";
import {takeUntil} from "rxjs/operators";

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
export class ChatVllmComponent implements OnDestroy, OnInit {

  readonly chatService = inject(ChatVllmService);

  modelName = input<string>();
  baseUrl = input<string>();
  enableThinkingInput = input<boolean>();
  advancedParamsInput = input<AdvancedParam[]>();
  supportsFiles = input.required<boolean>();
  acceptedFileTypes = input<('image' | 'video' | 'audio' | 'document')[]>();
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

  applicationId = input.required<string>();
  internalEndpoint = input.required<string>();
  private wsSubscription? : Subscription;
  private destroy$ = new Subject<void>();

  private readonly SUPPORTED_MIME_TYPES = {
    image: [
      'image/jpeg',
      'image/jpg',
      'image/png',
      'image/webp',
      'image/gif',
      'image/bmp'
    ],
    video: [
      'video/mp4',
      'video/webm',
      'video/quicktime', // .mov
      'video/x-msvideo'  // .avi
    ],
    audio: [
      'audio/mpeg',      // .mp3
      'audio/wav',       // .wav
      'audio/mp4',       // .m4a
      'audio/ogg',       // .ogg
      'audio/webm',      // .webm
      'audio/x-m4a',     // .m4a
      'audio/aac'        // .aac
    ],
    document: [
      'application/pdf',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document', // .docx
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',       // .xlsx
      'application/vnd.openxmlformats-officedocument.presentationml.presentation', // .pptx
      'application/msword',  // .doc
      'text/plain',          // .txt
      'text/markdown',       // .md
      'text/csv',            // .csv
      'application/json'     // .json
    ]
  };

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

  ngOnInit() {
    this.wsSubscription = this.chatService.connectToVllmChat(this.applicationId())
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (message) => {
          if (message && message.body) {
            try {
              const body = JSON.parse(message.body);
              if (body?.error) {
                this.streaming.set(false);
                this.errorMessage.set(String(body.error));
                this.live.set('');
                this.liveThinking.set('');
                return;
              }
            } catch (e) {
              // Ignore parse errors for body
            }
          }

          this.streaming.set(true);
          this.processMessage(message.binaryBody);
        },
        error: (err) => {
          console.error('WebSocket error:', err);
          this.streaming.set(false);
          this.errorMessage.set('WebSocket connection error');
        }
      });
  }

  private processMessage(binaryBody: Uint8Array): void {
    const messageRaw = new TextDecoder().decode(binaryBody);

    if (!messageRaw) {
      return;
    }

    try {
      const messageJson = JSON.parse(messageRaw);

      if (!messageJson) {
        return;
      }

      const payload = messageJson.payload;

      if (!payload) {
        return;
      }
      if (payload === '[ABORTED]') {
        this.streaming.set(false);
        return;
      }
      if (payload === '[DONE]') {
        if (messageJson.info) {
          this.info.set(messageJson.info);
        }
        this.finalizeMessage();
        return;
      }
      if (!this.streaming()) {
        console.log('⚠️ Received chunk after stop, ignoring:', payload);
        return;
      }

      const isThinking = messageJson.isThinking || false;

      if (isThinking) {
        this.liveThinking.update(v => v + payload);
      } else {
        this.live.update(v => v + payload);
      }

      this.scrollToBottom();

    } catch (e) {
      console.error('Failed to parse message:', messageRaw, e);
    }
  }

  private finalizeMessage() {
    let finalContent = this.live();
    let finalThinking = this.liveThinking();

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
    const acceptedTypes = this.acceptedFileTypes() || [];

    if (!acceptedTypes.includes(fileType)) {
      this.errorMessage.set(
        `File type "${fileType}" not accepted. Accepted types: ${acceptedTypes.join(', ')}`
      );
      return;
    }

    // ✅ Validate MIME type is in supported list
    const supportedMimeTypes = acceptedTypes.flatMap(type => this.SUPPORTED_MIME_TYPES[type] || []);
    if (!supportedMimeTypes.includes(file.type)) {
      this.errorMessage.set(
        `File format "${file.type}" is not supported. Please use: ${this.getAcceptedExtensions(acceptedTypes)}`
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
        `File "${file.name}" is too large. Maximum size: ${maxSize}MB (file is ${(file.size / 1024 / 1024).toFixed(2)}MB)`
      );
      return;
    }

    // ✅ Additional validation by type
    if (!this.validateFileByType(file, fileType)) {
      return; // Error already set in validateFileByType
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

      // ✅ Generate previews based on type
      if (fileType === 'image') {
        attachedFile.preview = await this.createImagePreview(file);
        attachedFile.url = attachedFile.preview;
      } else if (fileType === 'video') {
        attachedFile.preview = await this.createVideoThumbnail(file);
        attachedFile.url = URL.createObjectURL(file);
      } else if (fileType === 'document') {
        attachedFile.preview = this.getDocumentIcon(file);
      } else if (fileType === 'audio') {
        attachedFile.preview = this.getAudioIcon(file);
        attachedFile.url = URL.createObjectURL(file);
      }

      this.attachedFiles.update(files => [...files, attachedFile]);

    } catch (error) {
      console.error('Error processing file:', error);
      this.errorMessage.set(`Error processing file: ${file.name}`);
    }
  }

  // ✅ Helper: Get accepted extensions for display
  public getAcceptedExtensions(types: string[]): string {
    const extensionMap: Record<string, string> = {
      image: '.jpg, .png, .gif, .webp',
      video: '.mp4, .webm, .mov',
      audio: '.mp3, .wav, .m4a, .ogg',
      document: '.pdf, .docx, .txt, .md, .csv'
    };

    // ✅ FIXED: Type-safe mapping with proper type guard
    return types
      .map(type => extensionMap[type] || '')
      .filter(ext => ext !== '')
      .join(', ');
  }
// ✅ Additional validation by file type
  private validateFileByType(file: File, fileType: 'image' | 'audio' | 'video' | 'document'): boolean {
    // Image-specific validation
    if (fileType === 'image') {
      // Max resolution check (optional)
      return true; // Could add dimension check here
    }

    // Video-specific validation
    if (fileType === 'video') {
      const maxVideoSize = 100; // 100MB max for videos
      if (file.size > maxVideoSize * 1024 * 1024) {
        this.errorMessage.set(`Video files must be under ${maxVideoSize}MB`);
        return false;
      }
    }

    // Audio-specific validation
    if (fileType === 'audio') {
      const maxAudioSize = 25; // 25MB max for audio
      if (file.size > maxAudioSize * 1024 * 1024) {
        this.errorMessage.set(`Audio files must be under ${maxAudioSize}MB`);
        return false;
      }
    }

    // Document-specific validation
    if (fileType === 'document') {
      const maxDocSize = 50; // 50MB max for documents
      if (file.size > maxDocSize * 1024 * 1024) {
        this.errorMessage.set(`Document files must be under ${maxDocSize}MB`);
        return false;
      }
    }

    return true;
  }

  // ✅ Video thumbnail generation
  private createVideoThumbnail(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const video = document.createElement('video');
      video.preload = 'metadata';

      video.onloadedmetadata = () => {
        video.currentTime = 1; // Seek to 1 second for thumbnail
      };

      video.onseeked = () => {
        const canvas = document.createElement('canvas');
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;

        const ctx = canvas.getContext('2d');
        if (ctx) {
          ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
          resolve(canvas.toDataURL('image/jpeg', 0.8));
        } else {
          reject(new Error('Could not get canvas context'));
        }

        URL.revokeObjectURL(video.src);
      };

      video.onerror = () => reject(new Error('Error loading video'));
      video.src = URL.createObjectURL(file);
    });
  }

// ✅ Document icon based on extension
  private getDocumentIcon(file: File): string {
    const extension = file.name.toLowerCase().split('.').pop();

    // Return appropriate icon based on type
    const iconMap: Record<string, string> = {
      'pdf': '📄',
      'docx': '📝',
      'doc': '📝',
      'xlsx': '📊',
      'xls': '📊',
      'pptx': '📊',
      'txt': '📃',
      'md': '📋',
      'csv': '📈',
      'json': '📋'
    };

    return iconMap[extension || ''] || '📎';
  }

// ✅ Audio icon
  private getAudioIcon(file: File): string {
    return '🎵'; // Or use different icons based on format
  }

  /*private getFileType(file: File): 'image' | 'audio' | 'video' {
    if (file.type.startsWith('image/')) return 'image';
    if (file.type.startsWith('audio/')) return 'audio';
    if (file.type.startsWith('video/')) return 'video';
    return 'image'; // fallback
  }*/
  private getFileType(file: File): 'image' | 'audio' | 'video' | 'document' {
    const mimeType = file.type;

    // Check each category
    for (const [type, mimeTypes] of Object.entries(this.SUPPORTED_MIME_TYPES)) {
      if (mimeTypes.includes(mimeType)) {
        return type as 'image' | 'audio' | 'video' | 'document';
      }
    }

    // ✅ Fallback: Try to determine from extension
    const extension = file.name.toLowerCase().split('.').pop();

    if (extension) {
      if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(extension)) {
        return 'image';
      }
      if (['mp4', 'webm', 'mov', 'avi'].includes(extension)) {
        return 'video';
      }
      if (['mp3', 'wav', 'm4a', 'ogg', 'aac'].includes(extension)) {
        return 'audio';
      }
      if (['pdf', 'docx', 'doc', 'txt', 'md', 'csv', 'json', 'xlsx', 'pptx'].includes(extension)) {
        return 'document';
      }
    }

    // Default fallback
    console.warn(`Unknown file type for: ${file.name} (${mimeType})`);
    return 'document';
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

    this.chatService.sendChatMessage(
      this.applicationId(),
      baseUrl,
      model,
      this.messages(),
      this.advancedParams,
      this.systemPrompt,
      thinking,
      this.internalEndpoint()
    );
  }

  stop() {
    this.chatService.abortStream(this.applicationId());

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
    this.draft = '';

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

  // ✅ Generate HTML accept attribute
  getAcceptAttribute(): string {
    const acceptedTypes = this.acceptedFileTypes() || [];
    const mimeTypes = acceptedTypes.flatMap(type => this.SUPPORTED_MIME_TYPES[type] || []);
    return mimeTypes.join(',');
  }

// ✅ Format file size for display
  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
  }

  // ✅ Clean up object URLs on destroy
  ngOnDestroy() {
    // Cleanup WebSocket subscription
    this.destroy$.next();
    this.destroy$.complete();

    if (this.wsSubscription) {
      this.wsSubscription.unsubscribe();
    }

    // ✅ Revoke object URLs to prevent memory leaks
    this.attachedFiles().forEach(file => {
      if (file.url && file.url.startsWith('blob:')) {
        URL.revokeObjectURL(file.url);
      }
    });

    this.streaming.set(false);
  }

  clearError() {
    this.errorMessage.set(null);
  }
}
