"use client";

import { Header } from "@/components/Header";
import React, { useState } from "react";

interface Step {
  id: number;
  name: string;
  attempt: string;
  duration?: string;
  meta: string;
  status: "COMPLETED" | "RUNNING" | "PENDING";
  input?: Record<string, any>;
  output?: any;
  started?: string;
  ended?: string;
  signal?: string;
}

const STEPS: Step[] = [
  {
    id: 1,
    name: "fetch-order",
    attempt: "1/3",
    duration: "820ms",
    meta: "entry node",
    status: "COMPLETED",
  },
  {
    id: 2,
    name: "reserve-stock",
    attempt: "2/5",
    duration: "5.35s",
    meta: "after fetch-order",
    status: "COMPLETED",
  },
  {
    id: 3,
    name: "risk-check",
    attempt: "1/3",
    duration: "1.25s",
    meta: "after fetch-order",
    status: "COMPLETED",
  },
  {
    id: 4,
    name: "await-payment",
    attempt: "1/1",
    meta: "after reserve-stock, risk-check",
    status: "RUNNING",
    started: "2026-09-06 09:41:18Z",
    signal: "payment.captured",
    input: { signal: "payment.captured", timeoutSeconds: 900 },
    output: null,
  },
  {
    id: 5,
    name: "ship-order",
    attempt: "0/3",
    meta: "after await-payment",
    status: "PENDING",
  },
  {
    id: 6,
    name: "notify-customer",
    attempt: "0/4",
    meta: "after ship-order",
    status: "PENDING",
  },
];

export default function WorkflowConsole() {
  const [selectedStep, setSelectedStep] = useState<Step>(STEPS[3]);
  const [activeTab, setActiveTab] = useState("TIMELINE");

  return (
    <div className="min-h-screen bg-[#0b0f17] text-slate-100 font-sans antialiased flex flex-col">
      {/* Workflow Header Banner */}
      <div className="border-b border-slate-800/80 px-8 py-6 bg-[#0f141d]/50">
        <div className="text-[10px] uppercase tracking-widest text-slate-500 font-mono mb-1">
          Workflow
        </div>
        <div className="flex items-center justify-between flex-wrap gap-4">
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold tracking-tight text-slate-100">
              OrderFulfilment
            </h1>
            <span className="text-xs px-2 py-0.5 rounded bg-slate-800 text-slate-300 font-mono">
              v4
            </span>
            <span className="text-xs px-2.5 py-1 rounded-full bg-amber-500/10 text-amber-400 border border-amber-500/20 font-mono flex items-center gap-1.5">
              <span className="w-1.5 h-1.5 rounded-full bg-amber-400 animate-pulse" />
              AWAITING SIGNAL
            </span>
          </div>
          <div className="flex items-center gap-3">
            <button className="bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-medium px-4 py-2 rounded transition">
              Send signal
            </button>
            <button className="bg-slate-900 hover:bg-slate-800 border border-slate-700 text-slate-300 text-xs font-medium px-4 py-2 rounded transition">
              Cancel run
            </button>
          </div>
        </div>

        {/* Metadata Grid */}
        <div className="grid grid-cols-2 md:grid-cols-6 gap-6 mt-6 pt-6 border-t border-slate-800/60 text-xs font-mono">
          <div>
            <div className="text-slate-500 mb-1">INSTANCE ID</div>
            <div className="text-slate-300 truncate">
              wf_01J9XQ4K7ZC2MPBR3TN...
            </div>
          </div>
          <div>
            <div className="text-slate-500 mb-1">CURRENT STEP</div>
            <div className="text-emerald-400 font-medium">await-payment</div>
          </div>
          <div>
            <div className="text-slate-500 mb-1">STARTED</div>
            <div className="text-slate-300">2026-09-06 09:41:12Z</div>
          </div>
          <div>
            <div className="text-slate-500 mb-1">DURATION</div>
            <div className="text-slate-300">3m 4s</div>
          </div>
          <div>
            <div className="text-slate-500 mb-1">QUEUE</div>
            <div className="text-slate-300">taskflow:stream:default</div>
          </div>
          <div>
            <div className="text-slate-500 mb-1">LEADER</div>
            <div className="text-slate-300">node-a1 (epoch 17)</div>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="border-b border-slate-800/80 px-8 flex gap-8 text-xs font-mono">
        {[
          "TIMELINE",
          "EVENTS",
          "CONTEXT",
          "SIGNALS",
          "TIMERS",
          "HISTORY",
          "LOGS",
        ].map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`py-3.5 border-b-2 font-medium transition ${
              activeTab === tab
                ? "border-emerald-500 text-emerald-400"
                : "border-transparent text-slate-400 hover:text-slate-200"
            }`}
          >
            {tab}
          </button>
        ))}
      </div>

      {/* Main Content */}
      <main className="flex-1 p-8 grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* DAG Execution Column */}
        <div className="lg:col-span-2 bg-[#0f141d]/40 border border-slate-800/80 rounded-xl p-6 relative">
          <div className="text-[10px] uppercase tracking-widest text-slate-500 font-mono mb-6">
            DAG EXECUTION
          </div>

          <div className="relative pl-6 space-y-6 before:absolute before:left-[19px] before:top-3 before:bottom-3 before:w-0.5 before:bg-slate-800">
            {STEPS.map((step) => {
              const isSelected = selectedStep.id === step.id;
              return (
                <div
                  key={step.id}
                  onClick={() => setSelectedStep(step)}
                  className={`relative flex items-center justify-between p-4 rounded-lg border transition cursor-pointer ${
                    isSelected
                      ? "bg-slate-900/90 border-emerald-500/50 shadow-lg"
                      : "bg-[#0b0f17]/60 border-slate-800/80 hover:border-slate-700"
                  }`}
                >
                  <div className="flex items-center gap-4">
                    <div
                      className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-mono z-10 ${
                        step.status === "COMPLETED"
                          ? "bg-emerald-950 text-emerald-400 border border-emerald-800"
                          : step.status === "RUNNING"
                            ? "bg-cyan-950 text-cyan-400 border border-cyan-800 animate-pulse"
                            : "bg-slate-900 text-slate-500 border border-slate-800"
                      }`}
                    >
                      {step.id}
                    </div>
                    <div>
                      <div className="font-mono font-medium text-sm text-slate-200">
                        {step.name}
                      </div>
                      <div className="text-xs text-slate-500 font-mono mt-0.5">
                        attempt {step.attempt}{" "}
                        {step.duration && `· ${step.duration}`} · {step.meta}
                      </div>
                    </div>
                  </div>
                  <div>
                    <span
                      className={`text-[10px] px-2 py-1 rounded font-mono font-semibold ${
                        step.status === "COMPLETED"
                          ? "bg-emerald-950/80 text-emerald-400 border border-emerald-900"
                          : step.status === "RUNNING"
                            ? "bg-cyan-950/80 text-cyan-400 border border-cyan-900"
                            : "bg-slate-900 text-slate-500 border border-slate-800"
                      }`}
                    >
                      {step.status}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>

          <div className="mt-8 text-[11px] text-slate-600 font-mono">
            # demo data — this console is wired to fixtures, not a live engine
            yet
          </div>
        </div>

        {/* Step Detail Column */}
        <div className="bg-[#0f141d]/40 border border-slate-800/80 rounded-xl p-6 font-mono text-xs flex flex-col justify-between">
          <div>
            <div className="text-[10px] uppercase tracking-widest text-slate-500 mb-6">
              STEP DETAIL
            </div>

            <div className="text-base font-bold text-slate-200 mb-1">
              {selectedStep.name}
            </div>
            {selectedStep.signal && (
              <div className="text-slate-400 mb-6">
                signal:
                <span className="text-emerald-400">{selectedStep.signal}</span>
              </div>
            )}

            <div className="grid grid-cols-2 gap-4 mb-6 pt-4 border-t border-slate-800/80">
              <div>
                <div className="text-slate-500 text-[10px] mb-1">STARTED</div>
                <div className="text-slate-300">
                  {selectedStep.started || "—"}
                </div>
              </div>
              <div>
                <div className="text-slate-500 text-[10px] mb-1">ENDED</div>
                <div className="text-slate-300">
                  {selectedStep.ended || "—"}
                </div>
              </div>
            </div>

            <div className="mb-6">
              <div className="text-slate-500 text-[10px] mb-2">INPUT</div>
              <pre className="bg-[#0b0f17] p-3 rounded-lg border border-slate-800 text-slate-300 overflow-x-auto text-[11px]">
                {selectedStep.input
                  ? JSON.stringify(selectedStep.input, null, 2)
                  : "null"}
              </pre>
            </div>

            <div>
              <div className="text-slate-500 text-[10px] mb-2">OUTPUT</div>
              <div className="bg-[#0b0f17] p-3 rounded-lg border border-slate-800 text-slate-500">
                {selectedStep.output === null
                  ? "null"
                  : JSON.stringify(selectedStep.output)}
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
