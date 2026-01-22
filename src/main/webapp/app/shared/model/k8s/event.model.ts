export const SseEventTypeComplete = 'complete';
export const SseEventTypeTimeout = 'timeout';

export interface SseEvent<T> {
  type: string
  data?: T
}

