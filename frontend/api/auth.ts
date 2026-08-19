import { api } from "@/lib/api"

export async function signup(name: string, email: string, password: string, passwordConfirmation: string) {
  const response = await api.post("/auth/signup", {
    name,
    email,
    password,
    passwordConfirmation
  })
  return response.data
}
