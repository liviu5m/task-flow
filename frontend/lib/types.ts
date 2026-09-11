export type TaskStatus =
  "RUNNING" | "QUEUED" | "COMPLETED" | "FAILED" | "RETRY_WAITING";

export type ApiKey = {
  id: number;
  name: string;
  keyHash: string;
  userId: string;
  createdAt: string;
};

export type WorklfowInstance = {
  id: string;
  name: string;
  status: TaskStatus;
  userId: string;
  createdAt: string;
  events: Array<WorklfowInstanceEvent>;
  timers: Array<WorklfowInstanceTimer>;
  signals: Array<WorklfowInstanceSignal>;
};

export type WorklfowInstanceEvent = {
  id: string;
  seq: number;
  type: string;
  timestamp: string;
  payload: any;
};

export type WorklfowInstanceTimer = {
  id: string;
  fireAt: string;
  status: string;
};

export type WorklfowInstanceSignal = {
  name: string;
  receivedAt: string;
  payload: any;
};

export type WorklfowInstanceDetails = {
  id: string;
  name: string;
  status: TaskStatus;
  userId: string;
  createdAt: string;
  events: Array<WorklfowInstanceEvent>;
  timers: Array<WorklfowInstanceTimer>;
  signals: Array<WorklfowInstanceSignal>;
};
