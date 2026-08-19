export type TaskStatus =
  "RUNNING" | "QUEUED" | "COMPLETED" | "FAILED" | "RETRY_WAITING";

export interface Task {
  id: number;
  name: string;
  status: TaskStatus;
  priority: number;
  payloadJson: string;
  retryCount: number;
  maxRetries: number;
  errorDetails: string;
  createdAt: string;
  updatedAt: string;
  duration?: string;
  age?: string;
  desc?: string;
}
