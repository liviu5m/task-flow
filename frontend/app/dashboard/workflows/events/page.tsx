"use client";

import React, { useState } from "react";
import { useQuery, useMutation } from "@tanstack/react-query";
import { getApiKeys } from "@/api/apiKey";
import { getWorkflows, startWorkflowFunc } from "@/api/workflow";
import { useSession } from "@/lib/auth-client";
import { ApiKey } from "@/lib/types";
import { Loader } from "@/components/Loader";
import { Search, Key, Workflow, ChevronRight, Play, X } from "lucide-react";

export default function WorkflowsPage() {
  const { data: session } = useSession();
  const user = session?.user;

  const [selectedKeyId, setSelectedKeyId] = useState<number | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [activeWorkflow, setActiveWorkflow] = useState<string | null>(null);
  const [initialInput, setInitialInput] = useState('{\n  "input": "value"\n}');
  const [runResult, setRunResult] = useState<any>(null);

  const { data: apiKeys, isLoading: isLoadingKeys } = useQuery({
    queryKey: ["api-keys", user?.id],
    queryFn: () => getApiKeys(user?.id || ""),
    enabled: !!user?.id,
  });

  const { data: workflows, isLoading: isLoadingWorkflows } = useQuery({
    queryKey: ["workflows", selectedKeyId],
    queryFn: () => getWorkflows(selectedKeyId!),
    enabled: selectedKeyId !== null,
  });

  const { mutate: runWorkflow, isPending } = useMutation({
    mutationFn: ({ name, input }: { name: string; input: string }) =>
      startWorkflowFunc(name, input),
    onSuccess: (data) => {
      setRunResult(data);
    },
  });

  const filteredWorkflows = (workflows || []).filter((w: string) =>
    w.toLowerCase().includes(searchTerm.toLowerCase()),
  );

  if (isLoadingKeys) return <Loader />;

  return (
    <div className="max-w-4xl mx-auto space-y-6 font-mono text-xs">
      <div>
        <h1 className="text-xl font-bold text-slate-100">Workflows</h1>
        <p className="text-slate-400 mt-1">
          Select an API key, view workflows, click to run with initial input.
        </p>
      </div>

      <div className="bg-[#111827] border border-slate-800 rounded-xl p-5 space-y-4 shadow-xl">
        <label className="block text-slate-300 font-medium">
          Select API Key
        </label>
        {!apiKeys || apiKeys.length === 0 ? (
          <div className="p-4 bg-slate-900/50 border border-slate-800 rounded-lg text-slate-400">
            No API keys found.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {apiKeys.map((k: ApiKey) => (
              <button
                key={k.id}
                onClick={() => setSelectedKeyId(k.id)}
                className={`p-3 rounded-lg border text-left transition flex items-center justify-between ${
                  selectedKeyId === k.id
                    ? "bg-emerald-950/30 border-emerald-500/50 text-emerald-300 shadow-lg"
                    : "bg-slate-900/60 border-slate-800 text-slate-300 hover:border-slate-700"
                }`}
              >
                <div className="flex items-center gap-2.5">
                  <Key className="h-4 w-4 text-emerald-400" />
                  <div>
                    <div className="font-bold">{k.name}</div>
                    <div className="text-[10px] text-slate-500">ID: {k.id}</div>
                  </div>
                </div>
                <ChevronRight className="h-4 w-4 text-slate-500" />
              </button>
            ))}
          </div>
        )}
      </div>

      {selectedKeyId !== null && (
        <div className="bg-[#111827] border border-slate-800 rounded-xl p-5 space-y-4 shadow-xl">
          <div className="flex flex-col md:flex-row items-center justify-between gap-4">
            <h2 className="text-sm font-bold text-slate-100 flex items-center gap-2">
              <Workflow className="h-4 w-4 text-emerald-400" /> Registered
              Workflows
            </h2>
            <div className="relative w-full md:w-64">
              <Search className="absolute left-3 top-2.5 h-3.5 w-3.5 text-slate-500" />
              <input
                type="text"
                placeholder="Search workflows..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full bg-slate-900 border border-slate-800 rounded-lg pl-9 pr-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-emerald-500"
              />
            </div>
          </div>

          {isLoadingWorkflows ? (
            <div className="py-12 flex justify-center">
              <Loader />
            </div>
          ) : !filteredWorkflows || filteredWorkflows.length === 0 ? (
            <div className="p-8 text-center text-slate-400 bg-slate-900/40 rounded-lg border border-slate-800/60">
              No workflows found.
            </div>
          ) : (
            <div className="divide-y divide-slate-800/80 border border-slate-800 rounded-lg overflow-hidden bg-slate-900/20">
              {filteredWorkflows.map((workflowName: string) => (
                <div
                  key={workflowName}
                  onClick={() => {
                    setActiveWorkflow(workflowName);
                    setRunResult(null);
                  }}
                  className="p-3.5 flex items-center justify-between hover:bg-slate-800/40 transition cursor-pointer"
                >
                  <span className="font-medium text-slate-200">
                    {workflowName}
                  </span>
                  <button className="bg-emerald-600 hover:bg-emerald-500 text-white px-3 py-1 rounded text-xs flex items-center gap-1">
                    <Play className="h-3 w-3" /> Run
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Run Workflow Modal */}
      {activeWorkflow && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
          <div className="bg-[#111827] border border-slate-800 rounded-xl max-w-lg w-full p-6 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-bold text-slate-100 flex items-center gap-2">
                <Workflow className="h-4 w-4 text-emerald-400" /> Run Workflow:{" "}
                {activeWorkflow}
              </h3>
              <button
                onClick={() => setActiveWorkflow(null)}
                className="text-slate-400 hover:text-slate-100"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            <div>
              <label className="block text-slate-400 mb-1">
                Initial Input (JSON)
              </label>
              <textarea
                rows={5}
                value={initialInput}
                onChange={(e) => setInitialInput(e.target.value)}
                className="w-full bg-slate-900 border border-slate-800 rounded-lg p-3 text-slate-200 font-mono text-xs focus:outline-none focus:border-emerald-500"
              />
            </div>
            {runResult && (
              <div className="space-y-1">
                <div className="text-slate-400">Result:</div>
                <pre className="bg-slate-900 border border-slate-800 p-3 rounded text-emerald-400 overflow-x-auto text-[11px]">
                  {JSON.stringify(runResult, null, 2)}
                </pre>
              </div>
            )}
            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setActiveWorkflow(null)}
                className="bg-slate-800 hover:bg-slate-700 text-slate-300 px-4 py-2 rounded"
              >
                Close
              </button>
              <button
                disabled={isPending}
                onClick={() =>
                  runWorkflow({ name: activeWorkflow, input: initialInput })
                }
                className="bg-emerald-600 hover:bg-emerald-500 text-white px-4 py-2 rounded flex items-center gap-2"
              >
                <Play className="h-3.5 w-3.5" />{" "}
                {isPending ? "Starting..." : "Execute"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
