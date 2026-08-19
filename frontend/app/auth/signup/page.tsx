"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useMutation } from "@tanstack/react-query";
import { signup } from "@/api/auth";
import { AxiosError } from "axios";

export default function SignupPage() {
  const router = useRouter();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirmation, setPasswordConfirmation] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const { mutate: signupFunc } = useMutation({
    mutationKey: ["signup"],
    mutationFn: () => signup(name, email, password, passwordConfirmation),
    onSuccess: (data) => {
      console.log(data);
      router.push("/dashboard");
    },
    onError: (err: AxiosError) => {
      console.log(err.response);
      if (
        err?.response?.data ==
        "Account not verified, please verify your account"
      ) {
        // resend(data.email);
      }
      if (Array.isArray(err?.response?.data)) {
        setErrorMessage(
          err.response.data
            .map((msg: string, i: number) => `<div key="${i}">${msg}</div>`)
            .join(""),
        );
      } else setErrorMessage(err?.response?.data as string);
    },
  });

  const handleSignup = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage("");
    signupFunc();
  };

  const handleGoogleAuth = () => {
    router.push("/dashboard");
  };

  return (
    <main className="min-h-screen bg-[#0b0f17] text-slate-100 flex items-center justify-center font-sans p-6">
      <div className="w-full max-w-md p-8 bg-[#111827] border border-slate-800 rounded-2xl shadow-2xl">
        <div className="flex items-center justify-between mb-8">
          <div className="flex items-center gap-3">
            <div className="w-3 h-3 rounded-full bg-emerald-500 animate-pulse" />
            <h1 className="text-lg font-bold tracking-tight">TaskFlow</h1>
          </div>
          <Link
            href="/"
            className="text-xs font-mono text-slate-400 hover:text-slate-200"
          >
            ← Back
          </Link>
        </div>

        <h2 className="text-xl font-bold mb-2">Create an account</h2>
        <p className="text-sm text-slate-400 mb-6">
          Enter your details below to start queuing jobs.
        </p>

        {errorMessage && (
          <div className="mb-6 p-3 bg-red-950/50 border border-red-800/60 rounded-xl text-xs text-red-400 font-mono">
            {errorMessage}
          </div>
        )}

        <button
          onClick={handleGoogleAuth}
          className="w-full flex items-center justify-center gap-3 py-2.5 px-4 bg-slate-900 hover:bg-slate-800 border border-slate-700 rounded-xl text-sm font-medium transition-colors mb-6 text-slate-200 shadow-sm"
        >
          <svg className="w-4 h-4" viewBox="0 0 24 24">
            <path
              fill="#EA4335"
              d="M12 5c1.6 0 3 .6 4.1 1.6l3.1-3.1C17.3 1.8 14.8 1 12 1 7.4 1 3.5 3.6 1.6 7.4l3.7 2.9C6.2 7.3 8.9 5 12 5z"
            />
            <path
              fill="#4285F4"
              d="M23.5 12.3c0-.8-.1-1.6-.2-2.3H12v4.5h6.5c-.3 1.5-1.1 2.8-2.4 3.7l3.7 2.9c2.2-2 3.7-5 3.7-8.8z"
            />
            <path
              fill="#FBBC05"
              d="M5.3 14.7c-.2-.8-.4-1.7-.4-2.7s.2-1.9.4-2.7L1.6 6.4C.6 8.4 0 10.6 0 13s.6 4.6 1.6 6.6l3.7-2.9z"
            />
            <path
              fill="#34A853"
              d="M12 23c3.2 0 6-1.1 8-3l-3.7-2.9c-1.1.7-2.5 1.2-4.3 1.2-3.1 0-5.8-2.3-6.7-5.3L1.6 15.9C3.5 19.7 7.4 23 12 23z"
            />
          </svg>
          Continue with Google
        </button>

        <div className="relative mb-6">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-slate-800" />
          </div>
          <div className="relative flex justify-center text-xs uppercase">
            <span className="bg-[#111827] px-2 text-slate-500">
              Or with email
            </span>
          </div>
        </div>

        <form onSubmit={handleSignup} className="space-y-4">
          <div>
            <label className="block text-xs font-medium text-slate-400 mb-1">
              Full Name
            </label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2.5 text-sm text-slate-200 focus:outline-none focus:border-emerald-500 font-mono"
              placeholder="Alex Rivers"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-400 mb-1">
              Email address
            </label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2.5 text-sm text-slate-200 focus:outline-none focus:border-emerald-500 font-mono"
              placeholder="operator@internal.acme.dev"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-400 mb-1">
              Password
            </label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2.5 text-sm text-slate-200 focus:outline-none focus:border-emerald-500 font-mono"
              placeholder="••••••••••••"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-400 mb-1">
              Confirm Password
            </label>
            <input
              type="password"
              required
              value={passwordConfirmation}
              onChange={(e) => setPasswordConfirmation(e.target.value)}
              className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2.5 text-sm text-slate-200 focus:outline-none focus:border-emerald-500 font-mono"
              placeholder="••••••••••••"
            />
          </div>
          <button
            type="submit"
            className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white font-medium rounded-xl text-sm transition-colors mt-2 shadow-lg shadow-emerald-950/40"
          >
            Create Account
          </button>
        </form>

        <p className="text-center text-xs text-slate-400 mt-6">
          Already have an account?{" "}
          <Link href="/login" className="text-emerald-400 hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </main>
  );
}
