import { api } from "@/lib/api";

export async function createApiKey(name: string, userId: string) {
  const response = await api.post("/api/api-keys", {
    name,
    userId,
  });
  return response.data;
}

export async function deleteApiKey(id: number) {
  const response = await api.delete(`/api/api-keys/${id}`);
  return response.data;
}

export async function getApiKeys(userId: string) {
  const response = await api.get(`/api/api-keys?userId=${userId}`);
  return response.data;
}
