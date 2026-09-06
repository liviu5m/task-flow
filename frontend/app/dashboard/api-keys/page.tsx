"use client";

import React, { useState } from "react";
import { Copy, Plus, Trash2, Check, X, AlertTriangle } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createApiKey, deleteApiKey, getApiKeys } from "@/api/apiKey";
import { useSession } from "@/lib/auth-client";
import { ApiKey } from "@/lib/types";
import { Loader } from "@/components/Loader";
import { format, parseISO } from "date-fns";

export default function ApiKeysPage() {
  const { data } = useSession();
  const user = data?.user;
  const queryClient = useQueryClient();

  const { data: keys, isLoading } = useQuery({
    queryKey: ["api-keys", user?.id],
    queryFn: () => getApiKeys(user?.id || ""),
  });

  const { mutate: createKey } = useMutation({
    mutationKey: ["api-keys", "create"],
    mutationFn: ({ name }: { name: string }) =>
      createApiKey(name, user?.id || ""),
    onSuccess: (data) => {
      setName("");
      setIsCreateOpen(false);
      setNewlyCreatedKey(data.keyHash);
      setIsNewKeyModalOpen(true);
      queryClient.invalidateQueries({ queryKey: ["api-keys"] });
    },
    onError: (error) => {
      console.log(error);
    },
  });

  console.log(keys);

  const { mutate: deleteKey } = useMutation({
    mutationKey: ["api-keys", "delete"],
    mutationFn: ({ id }: { id: number }) => deleteApiKey(id),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["api-keys"] });
    },
    onError: (error) => {
      console.log(error);
    },
  });

  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [name, setName] = useState("");

  const [newlyCreatedKey, setNewlyCreatedKey] = useState<string | null>(null);
  const [isNewKeyModalOpen, setIsNewKeyModalOpen] = useState(false);
  const [copiedNew, setCopiedNew] = useState(false);

  const handleCopyNew = () => {
    if (newlyCreatedKey) {
      navigator.clipboard.writeText(newlyCreatedKey);
      setCopiedNew(true);
      setTimeout(() => setCopiedNew(false), 2000);
    }
  };

  const maskKey = (key: string) => {
    if (key.length <= 3) return key;
    return "•".repeat(key.length - 3) + key.slice(-3);
  };

  return isLoading ? (
    <Loader />
  ) : (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-slate-100 font-mono">
            API Keys
          </h1>
          <p className="text-xs text-slate-400 font-mono mt-1">
            Manage programmatic access tokens.
          </p>
        </div>
        <button
          onClick={() => setIsCreateOpen(true)}
          className="bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-medium px-4 py-2 rounded-lg transition font-mono flex items-center gap-1.5 shadow-lg shadow-emerald-950/40"
        >
          <Plus className="h-4 w-4" /> Create Key
        </button>
      </div>

      <div className="bg-[#111827] border border-slate-800 rounded-xl overflow-hidden font-mono text-xs">
        {!keys || keys.length === 0 ? (
          <div className="flex items-center justify-center p-8 bg-[#0b0f17]">
            <div className="flex items-center gap-3 font-mono text-xs text-slate-400">
              <span>No API keys found</span>
            </div>
          </div>
        ) : (
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-800 text-slate-400 bg-slate-900/40">
                <th className="py-3 px-4 font-medium">Name</th>
                <th className="py-3 px-4 font-medium">Token</th>
                <th className="py-3 px-4 font-medium">Created</th>
                <th className="py-3 px-4 font-medium text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-slate-300">
              {keys.map((k: ApiKey) => (
                <tr key={k.id} className="hover:bg-slate-800/40 transition">
                  <td className="py-3 px-4 font-medium text-slate-200">
                    {k.name}
                  </td>
                  <td className="py-3 px-4 text-slate-400 tracking-wider">
                    {maskKey(k.keyHash)}
                  </td>
                  <td className="py-3 px-4 text-slate-500">
                    {format(parseISO(k.createdAt), "yyyy-MM-dd HH:mm:ss")}
                  </td>
                  <td className="py-3 px-4 text-right flex items-center justify-end gap-2">
                    <button
                      onClick={() => deleteKey({ id: k.id })}
                      className="p-1.5 hover:bg-rose-950/50 rounded text-rose-400 hover:text-rose-300"
                      title="Delete"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {isCreateOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs">
          <div className="w-full max-w-md bg-[#111827] border border-slate-800 p-6 rounded-xl font-mono relative shadow-2xl">
            <div className="flex items-center justify-between pb-4 border-b border-slate-800 mb-6">
              <h2 className="text-sm font-bold text-slate-100">
                Create API Key
              </h2>
              <button
                onClick={() => setIsCreateOpen(false)}
                className="text-slate-400 hover:text-slate-100"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                createKey({ name });
              }}
              className="space-y-4"
            >
              <div>
                <label className="block text-xs text-slate-400 mb-1">
                  Key Name
                </label>
                <input
                  type="text"
                  placeholder="e.g. CI/CD Pipeline"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-emerald-500"
                  autoFocus
                />
              </div>
              <div className="pt-4 border-t border-slate-800 flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setIsCreateOpen(false)}
                  className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-slate-300 rounded-lg text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-medium"
                >
                  Generate Key
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Success / Reveal Modal */}
      {isNewKeyModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs">
          <div className="w-full max-w-md bg-[#111827] border border-slate-800 p-6 rounded-xl font-mono relative shadow-2xl space-y-4">
            <div className="flex items-center justify-between pb-4 border-b border-slate-800">
              <h2 className="text-sm font-bold text-slate-100">
                Save Your API Key
              </h2>
              <button
                onClick={() => setIsNewKeyModalOpen(false)}
                className="text-slate-400 hover:text-slate-100"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="flex items-start gap-3 p-3 bg-amber-500/10 border border-amber-500/20 rounded-lg text-amber-400 text-[11px]">
              <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5" />
              <span>
                This is the only time your full API key will be displayed. Copy
                it now; you will not be able to retrieve it later.
              </span>
            </div>

            <div className="flex items-center gap-2">
              <input
                type="text"
                readOnly
                value={newlyCreatedKey || ""}
                className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-emerald-400 font-mono select-all"
              />
              <button
                onClick={handleCopyNew}
                className="bg-emerald-600 hover:bg-emerald-500 text-white px-4 py-2 rounded-lg text-xs font-medium flex items-center gap-1.5 shrink-0 transition"
              >
                {copiedNew ? (
                  <Check className="h-4 w-4" />
                ) : (
                  <Copy className="h-4 w-4" />
                )}
                {copiedNew ? "Copied" : "Copy"}
              </button>
            </div>

            <div className="pt-4 border-t border-slate-800 flex justify-end">
              <button
                onClick={() => setIsNewKeyModalOpen(false)}
                className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-slate-300 rounded-lg text-xs"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
