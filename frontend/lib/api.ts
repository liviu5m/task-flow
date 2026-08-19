import axios from "axios";

const baseUrl = process.env.NEXT_PUBLIC_API_URL;

export const api = axios.create({
  baseURL: baseUrl,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});
