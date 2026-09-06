export type TaskStatus =
  "RUNNING" | "QUEUED" | "COMPLETED" | "FAILED" | "RETRY_WAITING";

export type ApiKey = {
  id: number;
  name: string;
  keyHash: string;
  userId: string;
  createdAt: string;
};
