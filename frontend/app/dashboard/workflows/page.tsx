"use client";

import React, { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { getApiKeys } from "@/api/apiKey";
import { getWorkflows } from "@/api/workflow";
import { useSession } from "@/lib/auth-client";
import { ApiKey } from "@/lib/types";
import { Loader } from "@/components/Loader";
import { Search, Key, Workflow, ChevronRight } from "lucide-react";

export default function WorkflowsPage() {
  const { data: session } = useSession();
  const user = session?.user;

  const [selectedKeyId, setSelectedKeyId] = useState<number | null>(null);
  const [searchTerm, setSearchTerm] = useState("");

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

  const filteredWorkflows = (workflows || []).filter((w: string) =>
    w.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (isLoadingKeys) return <Loader />;

  return (
    <div className="max-w-4xl mx-auto space-y-6 font-mono text-xs">
      <div>
        <h1 className="text-xl font-bold text-slate-100">Workflows</h1>
        <p className="text-slate-400 mt-1">
          Select an API key to view and search registered workflows.
        </p>
      </div>

      <div className="bg-[#111827] border border-slate-800 rounded-xl p-5 space-y-4 shadow-xl">
        <label className="block text-slate-300 font-medium">
          Select API Key
        </label>
        {!apiKeys || apiKeys.length === 0 ? (
          <div className="p-4 bg-slate-900/50 border border-slate-800 rounded-lg text-slate-400">
            No API keys found. Please create an API key first in the API Keys section.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {apiKeys.map((k: ApiKey) => (
              <button
                key={k.id}
                onClick={() => setSelectedKeyId(k.id)}
                className={`p-3 rounded-lg border text-left transition flex items-center justify-between ${
                  selectedKeyId === k.id
                    ? "bg-emerald-950/30 border-emerald-500/50 text-emerald-300 shadow-lg shadow-emerald-950/20"
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
              <Workflow className="h-4 w-4 text-emerald-400" />
              Workflows for Selected Key
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
              No workflows found matching your search.
            </div>
          ) : (
            <div className="divide-y divide-slate-800/80 border border-slate-800 rounded-lg overflow-hidden bg-slate-900/20">
              {filteredWorkflows.map((workflowName: string) => (
                <div
                  key={workflowName}
                  className="p-3.5 flex items-center justify-between hover:bg-slate-800/40 transition"
                >
                  <span className="font-medium text-slate-200">{workflowName}</span>
                  <span className="text-[10px] bg-slate-800 text-slate-300 px-2 py-1 rounded">
                    Active
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
