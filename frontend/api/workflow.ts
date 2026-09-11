import { api } from "@/lib/api";

export async function getWorkflows(apiKeyId: number) {
  const response = await api.get(`/api/workflow/?apiKeyId=${apiKeyId}`);
  return response.data;
}

export async function startWorkflowFunc(workflowName: string, input: string) {
  const initialInput = typeof input === "string" ? JSON.parse(input) : input;
  const response = await api.post(`/api/workflow/start/${workflowName}`, {
    initialInput,
  });
  return response.data;
}

export async function getWorkflowInstances() {
  const response = await api.get(`/api/workflow/instances`);
  return response.data;
}
