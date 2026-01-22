import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {Observable} from 'rxjs';

export interface AttachedFile {
  name: string;
  size: number;
  type: 'image' | 'audio' | 'video';
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

export interface MessageInfo {
  finishReason?: string;
  model?: string;
  timestamp?: Date;
  usage?: {
    inputTokens?: number;
    outputTokens?: number;
    totalTokens?: number;
  };
  provider?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ChatVllmService {

  constructor(private http: HttpClient) {
  }

  streamChat$(
    baseUrl: string,
    model: string,
    messages: ChatMessage[],
    advancedParams: AdvancedParam[],
    systemPrompt: string,
    enableThinking: boolean = false
  ): Observable<{ chunk: string; thinking?: string; info?: any; isThinking?: boolean }> {
    return new Observable(observer => {
      const abortController = new AbortController();

      (async () => {
        try {

          const apiUrl = `${baseUrl}/chat/completions`;

          const body: any = {
            model: model,
            messages: this.buildMessages(messages, systemPrompt),
            stream: true,
            stream_options: {include_usage: true},
            chat_template_kwargs: {
              enable_thinking: enableThinking
            },
            ...this.buildAdvancedOptions(advancedParams)
          };

          console.log("body: " + JSON.stringify(body));
          console.log("apiUrl: " + apiUrl);

          const response = await fetch(apiUrl, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify(body),
            signal: abortController.signal,
          });

          if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
          }

          if (!response.body) {
            throw new Error('No response body');
          }

          const reader = response.body.getReader();
          const decoder = new TextDecoder();
          let buffer = '';
          let usageInfo: any = null;
          let finishReason: string | undefined;
          let chunkCount = 0;
          let isInThinking = false;
          let thinkingBuffer = '';
          let completeContent = '';


          while (true) {
            const {done, value} = await reader.read();

            if (done) {
              break;
            }

            buffer += decoder.decode(value, {stream: true});
            const lines = buffer.split('\n');

            buffer = lines.pop() || '';

            for (const line of lines) {
              const trimmed = line.trim();
              if (!trimmed || !trimmed.startsWith('data: ')) continue;

              const data = trimmed.slice(6);

              if (data === '[DONE]') {
                continue;
              }

              try {
                const parsed = JSON.parse(data);
                let content = parsed.choices?.[0]?.delta?.content;

                if (content !== undefined && content !== null && content !== '') {
                  chunkCount++;
                  completeContent += content;
                  const thinkStartMatch = content.match(/<think>/i);
                  const thinkEndMatch = content.match(/<\/think>/i);

                  if (thinkStartMatch) {
                    isInThinking = true;
                    const parts = content.split(/<think>/i);
                    if (parts[0]) {
                      observer.next({chunk: parts[0], isThinking: false});
                    }
                    thinkingBuffer = parts[1] || '';
                    content = '';
                  } else if (thinkEndMatch) {
                    isInThinking = false;
                    const parts = content.split(/<\/think>/i);
                    thinkingBuffer += parts[0];

                    thinkingBuffer = '';  // Clear buffer
                    if (parts[1]) {
                      observer.next({chunk: parts[1], isThinking: false});
                    }
                    content = '';
                  } else if (isInThinking) {
                    thinkingBuffer += content;
                    observer.next({chunk: '', thinking: content, isThinking: true});
                    content = '';
                  }

                  if (content) {
                    observer.next({chunk: content, isThinking: false});
                  }
                }

                const currentFinishReason = parsed.choices?.[0]?.finish_reason;
                if (currentFinishReason) {
                  finishReason = currentFinishReason;
                }

                if (parsed.usage) {
                  usageInfo = {
                    inputTokens: parsed.usage.prompt_tokens || 0,
                    outputTokens: parsed.usage.completion_tokens || 0,
                    totalTokens: parsed.usage.total_tokens || 0,
                  };
                }

              } catch (parseError) {
                console.error('❌ Parse error:', parseError);
              }
            }
          }


          if (enableThinking && completeContent.includes('<think>')) {
            console.log('⚠️ Think tags found in complete content - post-processing');
          }

          const finalInfo: MessageInfo = {
            finishReason: finishReason || 'stop',
            model: model,
            timestamp: new Date(),
            usage: usageInfo || undefined,
            provider: 'vllm'
          };

          console.log('📋 Final info:', finalInfo);
          observer.next({chunk: '', info: finalInfo});
          observer.complete();

        } catch (error: any) {
          console.error('❌ Error:', error);
          if (error.name === 'AbortError') {
            observer.complete();
          } else {
            observer.error(this.enhanceError(error));
          }
        }
      })();

      return () => {
        abortController.abort();
      };
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

        // Add files
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

  private enhanceError(error: any): Error {
    let message = 'An error occurred while processing your request.';
    const status = error.status || error.statusCode;
    if (status === 401) message = 'Invalid API key. Please check your configuration.';
    else if (status === 429) message = 'Rate limit exceeded. Please wait a moment and try again.';
    else if (status === 404) message = 'Model not found. Please check the model name.';
    else if (status === 500) message = 'Server error. Please try again later.';
    else if (status >= 400 && status < 500) message = `Client error (${status}). Please check your request.`;
    else if (status >= 500) message = `Server error (${status}). Please try again later.`;
    else if (error.message?.toLowerCase().includes('network') || error.message?.toLowerCase().includes('fetch')) {
      message = 'Network error. Please check your connection and try again.';
    } else if (error.message) message = error.message;

    const enhancedError = new Error(message);
    (enhancedError as any).originalError = error;
    (enhancedError as any).status = status;
    return enhancedError;
  }
}
