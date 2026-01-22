export interface DbStatus {
  stage?: string;  // RUNNING, WAITING, INITIALIZING, etc.
  message?: string;
  details?: string[];
  type?: string;
  error?: string;
  databaseId?: string;
}
