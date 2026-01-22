export type TimeRange = 'none' | '5m' | '15m' | '1h' | '6h';

export interface ILog {
  id?: number;
  timestamp: string;
  message: string;
  type?: string;
  error?: string;
}

export interface ILogCriteria {
  applicationId: string;
  podName: string;
  limit: number;
  containerId?: string;
  searchTerm?: string;
  startDate?: Date;
  endDate?: Date;
  sinceSeconds?: number;
}

export interface TimeRangeOption {
  label: string;
  value: TimeRange;
  seconds: number | undefined;
}
