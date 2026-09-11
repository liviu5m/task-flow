"use client";

import React, { useState } from "react";
import {
  Activity,
  Clock,
  Terminal,
  AlertCircle,
  CheckCircle,
  ChevronRight,
  Filter,
} from "lucide-react";
import { getWorkflowInstances } from "@/api/workflow";
import { useQuery } from "@tanstack/react-query";

interface WorkflowInstance {
  id: string;
  workflowName: string;
  status: "COMPLETED" | "RUNNING" | "FAILED";
  startedAt: string;
  duration: string;
  events: Array<{ seq: number; type: string; timestamp: string; payload: any }>;
  timers: Array<{ id: string; fireAt: string; status: string }>;
  signals: Array<{ name: string; receivedAt: string; payload: any }>;
}

const MOCK_INSTANCES: WorkflowInstance[] = [
  {
    id: "wf_inst_9981abc",
    workflowName: "OrderFulfilment",
    status: "RUNNING",
    startedAt: "2026-09-06 10:00:00Z",
    duration: "4m 12s",
    events: [
      {
        seq: 1,
        type: "WORKFLOW_STARTED",
        timestamp: "10:00:00Z",
        payload: { orderId: "ORD-999" },
      },
      {
        seq: 2,
        type: "STEP_COMPLETED",
        timestamp: "10:00:02Z",
        payload: { step: "fetch-order" },
      },
      {
        seq: 3,
        type: "SIGNAL_AWAITING",
        timestamp: "10:00:03Z",
        payload: { signal: "payment.captured" },
      },
    ],
    timers: [
      { id: "timer_1", fireAt: "2026-09-06 10:15:00Z", status: "ACTIVE" },
    ],
    signals: [
      {
        name: "payment.captured",
        receivedAt: "Pending",
        payload: { timeoutSeconds: 900 },
      },
    ],
  },
  {
    id: "wf_inst_5542xyz",
    workflowName: "UserOnboarding",
    status: "COMPLETED",
    startedAt: "2026-09-06 09:15:00Z",
    duration: "1m 05s",
    events: [
      {
        seq: 1,
        type: "WORKFLOW_STARTED",
        timestamp: "09:15:00Z",
        payload: { userId: "U-123" },
      },
      {
        seq: 2,
        type: "STEP_COMPLETED",
        timestamp: "09:15:05Z",
        payload: { step: "send-welcome-email" },
      },
      {
        seq: 3,
        type: "WORKFLOW_COMPLETED",
        timestamp: "09:16:05Z",
        payload: { result: "success" },
      },
    ],
    timers: [],
    signals: [],
  },
];

export default function InstancesDashboardPage() {
  const [selectedInstance, setSelectedInstance] =
    useState<WorkflowInstance | null>(MOCK_INSTANCES[0]);
  const { data: instances, isLoading: isLoadingInstances } = useQuery({
    queryKey: ["workflow-instances"],
    queryFn: () => getWorkflowInstances(),
    enabled: true,
  });
  console.log(instances);

  const [activeTab, setActiveTab] = useState<"EVENTS" | "TIMERS" | "SIGNALS">(
    "EVENTS",
  );
  const [filterType, setFilterType] = useState("");

  return (
    <div className="max-w-7xl mx-auto space-y-6 font-mono text-xs p-6">
      <div>
        <h1 className="text-xl font-bold text-slate-100 flex items-center gap-2">
          <Activity className="h-5 w-5 text-emerald-400" /> Workflow Instances
          Dashboard
        </h1>
        <p className="text-slate-400 mt-1">
          Select an instance to inspect fully detailed events, timers, and
          signals.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Instances List */}
        <div className="bg-[#111827] border border-slate-800 rounded-xl p-4 space-y-3 shadow-xl">
          <div className="text-slate-400 font-bold mb-2">
            Instances ({MOCK_INSTANCES.length})
          </div>
          <div className="space-y-2">
            {MOCK_INSTANCES.map((inst) => (
              <div
                key={inst.id}
                onClick={() => setSelectedInstance(inst)}
                className={`p-3 rounded-lg border cursor-pointer transition flex items-center justify-between ${
                  selectedInstance?.id === inst.id
                    ? "bg-emerald-950/30 border-emerald-500/50 text-emerald-300 shadow-lg"
                    : "bg-slate-900/60 border-slate-800 text-slate-300 hover:border-slate-700"
                }`}
              >
                <div>
                  <div className="font-bold text-slate-100">
                    {inst.workflowName}
                  </div>
                  <div className="text-[10px] text-slate-500">{inst.id}</div>
                </div>
                <div className="text-right">
                  <span
                    className={`text-[10px] px-2 py-0.5 rounded ${
                      inst.status === "COMPLETED"
                        ? "bg-emerald-950 text-emerald-400"
                        : "bg-amber-950 text-amber-400"
                    }`}
                  >
                    {inst.status}
                  </span>
                  <div className="text-[10px] text-slate-500 mt-1">
                    {inst.duration}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Instance Details / Event Stream Viewer */}
        <div className="lg:col-span-2 bg-[#111827] border border-slate-800 rounded-xl p-6 space-y-4 shadow-xl">
          {selectedInstance ? (
            <>
              <div className="flex items-center justify-between border-b border-slate-800 pb-4">
                <div>
                  <div className="text-sm font-bold text-slate-100">
                    {selectedInstance.workflowName}
                  </div>
                  <div className="text-[10px] text-slate-500">
                    ID: {selectedInstance.id} | Started:{" "}
                    {selectedInstance.startedAt}
                  </div>
                </div>
                <span className="text-xs px-2.5 py-1 rounded bg-slate-900 border border-slate-800 text-emerald-400">
                  {selectedInstance.status}
                </span>
              </div>

              {/* Tabs */}
              <div className="flex gap-4 border-b border-slate-800 pb-2 text-xs">
                {(["EVENTS", "TIMERS", "SIGNALS"] as const).map((tab) => (
                  <button
                    key={tab}
                    onClick={() => setActiveTab(tab)}
                    className={`font-medium pb-1 border-b-2 transition ${
                      activeTab === tab
                        ? "border-emerald-500 text-emerald-400"
                        : "border-transparent text-slate-400 hover:text-slate-200"
                    }`}
                  >
                    {tab} (
                    {tab === "EVENTS"
                      ? selectedInstance.events.length
                      : tab === "TIMERS"
                        ? selectedInstance.timers.length
                        : selectedInstance.signals.length}
                    )
                  </button>
                ))}
              </div>

              {/* Tab Content */}
              {activeTab === "EVENTS" && (
                <div className="space-y-3">
                  <div className="flex items-center gap-2">
                    <Filter className="h-3.5 w-3.5 text-slate-500" />
                    <input
                      type="text"
                      placeholder="Filter event types..."
                      value={filterType}
                      onChange={(e) => setFilterType(e.target.value)}
                      className="bg-slate-900 border border-slate-800 rounded px-2 py-1 text-xs text-slate-200 w-full focus:outline-none focus:border-emerald-500"
                    />
                  </div>
                  <div className="divide-y divide-slate-800/80 border border-slate-800 rounded-lg overflow-hidden bg-slate-900/30">
                    {selectedInstance.events
                      .filter((ev) =>
                        ev.type
                          .toLowerCase()
                          .includes(filterType.toLowerCase()),
                      )
                      .map((ev) => (
                        <div key={ev.seq} className="p-3 space-y-1">
                          <div className="flex items-center justify-between">
                            <span className="text-emerald-400 font-bold">
                              #{ev.seq} {ev.type}
                            </span>
                            <span className="text-[10px] text-slate-500">
                              {ev.timestamp}
                            </span>
                          </div>
                          <pre className="bg-slate-950 p-2 rounded border border-slate-900 text-slate-300 overflow-x-auto text-[10px]">
                            {JSON.stringify(ev.payload, null, 2)}
                          </pre>
                        </div>
                      ))}
                  </div>
                </div>
              )}

              {activeTab === "TIMERS" && (
                <div className="space-y-2">
                  {selectedInstance.timers.length === 0 ? (
                    <div className="p-6 text-center text-slate-500">
                      No active timers.
                    </div>
                  ) : (
                    selectedInstance.timers.map((t) => (
                      <div
                        key={t.id}
                        className="p-3 bg-slate-900/50 border border-slate-800 rounded flex items-center justify-between"
                      >
                        <div>
                          <div className="font-bold text-slate-200">{t.id}</div>
                          <div className="text-[10px] text-slate-500">
                            Fire at: {t.fireAt}
                          </div>
                        </div>
                        <span className="text-cyan-400 bg-cyan-950/40 border border-cyan-900 px-2 py-0.5 rounded text-[10px]">
                          {t.status}
                        </span>
                      </div>
                    ))
                  )}
                </div>
              )}

              {activeTab === "SIGNALS" && (
                <div className="space-y-2">
                  {selectedInstance.signals.length === 0 ? (
                    <div className="p-6 text-center text-slate-500">
                      No signals registered.
                    </div>
                  ) : (
                    selectedInstance.signals.map((s, idx) => (
                      <div
                        key={idx}
                        className="p-3 bg-slate-900/50 border border-slate-800 rounded space-y-1"
                      >
                        <div className="flex items-center justify-between">
                          <span className="font-bold text-amber-400">
                            {s.name}
                          </span>
                          <span className="text-[10px] text-slate-500">
                            {s.receivedAt}
                          </span>
                        </div>
                        <pre className="bg-slate-950 p-2 rounded border border-slate-900 text-slate-300 text-[10px]">
                          {JSON.stringify(s.payload, null, 2)}
                        </pre>
                      </div>
                    ))
                  )}
                </div>
              )}
            </>
          ) : (
            <div className="py-24 text-center text-slate-500">
              Select an instance to view details.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
