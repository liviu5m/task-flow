"use client";

import Link from "next/link";

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-[#0b0f17] text-slate-100 flex flex-col justify-between font-sans antialiased selection:bg-emerald-500 selection:text-white">
      <header className="w-full border-b border-slate-800/80 px-8 h-20 flex items-center justify-between backdrop-blur-md bg-[#0b0f17]/80 sticky top-0 z-50">
        <div className="flex items-center gap-3">
          <div className="w-3 h-3 rounded-full bg-emerald-500 animate-pulse" />
          <span className="font-bold tracking-tight text-lg">TaskFlow</span>
          <span className="text-xs px-2 py-0.5 rounded bg-slate-800/80 text-slate-400 font-mono">
            v0.4
          </span>
        </div>
        <div className="flex items-center gap-4">
          <Link
            href="/auth/login"
            className="text-sm font-medium text-slate-300 hover:text-slate-100 px-4 py-2 transition-colors"
          >
            Sign In
          </Link>
          <Link
            href="/auth/signup"
            className="text-sm font-medium bg-emerald-600 hover:bg-emerald-500 text-white px-4 py-2 rounded-lg transition-colors shadow-lg shadow-emerald-950/50"
          >
            Get Started
          </Link>
        </div>
      </header>

      {/* Hero Section */}
      <main className="flex-1 flex flex-col items-center justify-center text-center px-6 py-20 relative overflow-hidden">
        {/* Subtle background glow effect */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[300px] bg-emerald-500/10 blur-[120px] rounded-full pointer-events-none" />

        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-slate-900 border border-slate-800 text-xs text-emerald-400 font-mono mb-6">
          <span>⚡ Powered by Java 21 Virtual Threads & PostgreSQL</span>
        </div>

        <h1 className="text-5xl md:text-7xl font-extrabold tracking-tight max-w-4xl text-slate-100 mb-6 leading-tight">
          Engineer and scale{" "}
          <span className="text-transparent bg-clip-text bg-gradient-to-r from-emerald-400 to-cyan-400">
            queue systems
          </span>{" "}
          with clarity.
        </h1>

        <p className="text-lg text-slate-400 max-w-2xl mb-10 font-normal">
          High-performance distributed asynchronous job orchestration engine
          built for mission-critical operations. Monitor throughput, handle
          automatic retries, and manage concurrency seamlessly.
        </p>

        <div className="flex items-center gap-4">
          <Link
            href="/signup"
            className="px-6 py-3 bg-emerald-600 hover:bg-emerald-500 text-white font-medium rounded-xl text-sm transition-all shadow-xl shadow-emerald-950/60"
          >
            Deploy Control Plane →
          </Link>
          <Link
            href="/login"
            className="px-6 py-3 bg-slate-900 hover:bg-slate-800 border border-slate-700 text-slate-200 font-medium rounded-xl text-sm transition-all"
          >
            Access Dashboard
          </Link>
        </div>
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-800 py-6 px-8 text-center text-xs text-slate-500 font-mono">
        TaskFlow Enterprise Queue Infrastructure © 2026. All rights reserved.
      </footer>
    </div>
  );
}
