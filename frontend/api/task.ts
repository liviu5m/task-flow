import { api } from "@/lib/api";

export async function createTask() {
  const response = await api.post("/api/tasks/create");
  return response.data;
}
