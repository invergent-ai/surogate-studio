import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TrackerService } from './tracker.service';

export interface AttachedFile {
  name: string;
  size: number;
  type: 'image' | 'audio' | 'video' | 'document';
  base64: string;
  preview?: string;
  mimeType: string;
  url?: string; // For display purposes
}

export type ChatMessage =
  | { role: 'system'; content: string; info?: any }
  | { role: 'user'; content: string; files?: AttachedFile[]; info?: any }
  | { role: 'assistant'; content: string; thinking?: string; thinkingExpanded?: boolean; info?: any };

type ParamType = 'string' | 'number' | 'boolean' | 'list' | 'enum';

export interface AdvancedParam {
  label: string;
  key: string;
  type: ParamType;
  value: string | number | boolean | string[] | null;
  isCustom: boolean;
  options?: string[];
}

@Injectable({
  providedIn: 'root'
})
export class ChatVllmService {

  constructor(private http: HttpClient, private trackerService: TrackerService) {
  }

  connectToVllmChat(applicationId: string): Observable<any> {
    const topic = `/topic/message/${applicationId}`;
    return this.trackerService.stomp.watch(topic);
  }

  sendChatMessage(
    applicationId: string,
    baseUrl: string,
    model: string,
    messages: ChatMessage[],
    advancedParams: AdvancedParam[],
    systemPrompt: string,
    enableThinking: boolean = false,
    internalEndpoint: string
  ): void {
    const payload = {
      applicationId: applicationId,
      baseUrl: baseUrl,
      model: model,
      messages: this.buildMessages(messages, systemPrompt),
      advancedParams: this.buildAdvancedOptions(advancedParams),
      systemPrompt: systemPrompt,
      enableThinking: enableThinking,
      internalEndpoint: internalEndpoint
    };
    this.trackerService.stomp.publish({
      destination: '/job/message',
      body: JSON.stringify(payload)
    });
  }

  abortStream(applicationId: string): void {

    this.trackerService.stomp.publish({
      destination: '/job/message',
      body: JSON.stringify({
        applicationId: applicationId,
        abort: true
      })
    });
  }

  private buildMessages(messages: ChatMessage[], systemPrompt: string): any[] {
    const result: any[] = [];

    if (systemPrompt?.trim()) {
      result.push({role: 'system', content: systemPrompt});
    }

    for (const msg of messages) {
      if (msg.role === 'system' && systemPrompt?.trim()) continue;

      if (msg.role === 'user' && msg.files && msg.files.length > 0) {
        // Build message with files (multimodal)
        const content: any[] = [];

        // Add text content if exists
        if (msg.content && msg.content.trim()) {
          content.push({
            type: 'text',
            text: msg.content
          });
        }

        // ✅ Add files with proper handling for each type
        for (const file of msg.files) {
          if (file.type === 'image') {
            content.push({
              type: 'image_url',
              image_url: {
                url: `data:${file.mimeType};base64,${file.base64}`
              }
            });
          } else if (file.type === 'audio') {
            content.push({
              type: 'audio',
              audio: {
                format: file.mimeType.split('/')[1], // e.g., 'mp3', 'wav'
                data: file.base64
              }
            });
          } else if (file.type === 'video') {
            content.push({
              type: 'video',
              video: {
                format: file.mimeType.split('/')[1], // e.g., 'mp4', 'webm'
                data: file.base64
              }
            });
          } else if (file.type === 'document') {
            // ✅ NEW: Handle document files
            // Documents need to be extracted/converted first
            // Most vision models don't support documents directly

            if (file.mimeType === 'application/pdf') {
              // ✅ Option 1: Send as PDF (if model supports it)
              content.push({
                type: 'document',
                document: {
                  format: 'pdf',
                  data: file.base64
                }
              });
            } else if (file.mimeType === 'text/plain' ||
              file.mimeType === 'text/markdown' ||
              file.mimeType === 'text/csv' ||
              file.mimeType === 'application/json') {
              // ✅ Option 2: Decode text files and include as text
              try {
                const decodedText = this.decodeBase64ToText(file.base64);
                content.push({
                  type: 'text',
                  text: `[File: ${file.name}]\n${decodedText}`
                });
              } catch (e) {
                console.error('Failed to decode text file:', e);
                content.push({
                  type: 'text',
                  text: `[Error reading file: ${file.name}]`
                });
              }
            } else {
              // ✅ Option 3: For unsupported documents, add placeholder
              // In production, you'd want to extract text/images on backend
              content.push({
                type: 'text',
                text: `[Document attached: ${file.name} (${file.mimeType})]`
              });
            }
          }
        }

        result.push({
          role: msg.role,
          content: content
        });
      } else {
        // Simple text message
        result.push({
          role: msg.role,
          content: msg.content
        });
      }
    }

    return result;
  }

// ✅ NEW: Helper method to decode base64 to text
  private decodeBase64ToText(base64: string): string {
    try {
      // Decode base64 to binary string
      const binaryString = atob(base64);

      // Convert binary string to UTF-8
      const bytes = new Uint8Array(binaryString.length);
      for (let i = 0; i < binaryString.length; i++) {
        bytes[i] = binaryString.charCodeAt(i);
      }

      const decoder = new TextDecoder('utf-8');
      return decoder.decode(bytes);
    } catch (e) {
      throw new Error('Failed to decode base64 to text');
    }
  }

  private buildAdvancedOptions(params: AdvancedParam[]): Record<string, any> {
    const options: Record<string, any> = {};
    for (const p of params) {
      if (p.value === null || p.value === '' || p.value === undefined) continue;
      if (p.type === 'list') {
        options[p.key] = Array.isArray(p.value) ? p.value : String(p.value).split(',').map(s => s.trim());
        continue;
      }
      if (p.type === 'number') {
        options[p.key] = Number(p.value);
        continue;
      }
      options[p.key] = p.value;
    }
    return options;
  }
}
