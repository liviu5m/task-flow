"use client";

import { useState } from "react";
import Link from "next/link";
import { authClient } from "@/lib/auth-client";
import toast from "react-hot-toast";
import { useRouter } from "next/navigation";

export default function SignupPage() {
  const router = useRouter();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirmation, setPasswordConfirmation] = useState("");

  const handleSignup = async (e: React.FormEvent) => {
    e.preventDefault();
    await authClient.signUp.email(
      {
        email,
        password,
        name,
      },
      {
        onSuccess: (data) => {
          toast.success(
            "Account created! Please check your email to verify your account.",
          );
          router.push("/auth/login");
          console.log(data);
        },
        onError: (error) => {
          toast.error(error.error.message);
          console.log(error);
        },
      },
    );
  };

  const handleGoogleAuth = async () => {
    await authClient.signIn.social(
      {
        provider: "google",
        callbackURL: "/dashboard",
      },
      {
        onError: (ctx) => {
          toast(ctx.error.message);
        },
      },
    );
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

        <button
          onClick={handleGoogleAuth}
          className="w-full flex items-center justify-center gap-3 py-2.5 px-4 bg-slate-900 hover:bg-slate-800 border border-slate-700 rounded-xl text-sm font-medium transition-colors mb-6 text-slate-200 shadow-sm cursor-pointer"
        >
          <img src={"/google.png"} className="w-4 h-4" alt="google" />
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
          <Link href="/auth/login" className="text-emerald-400 hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </main>
  );
}
