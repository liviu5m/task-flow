// app/dashboard/page.tsx
"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

interface Task {
  id: number;
  name: string;
  status: "RUNNING" | "COMPLETED" | "FAILED" | "RETRY_WAITING" | "QUEUED";
  priority: number;
  category: string;
  payloadJson: string;
  retryCount: number;
  maxRetries: number;
  errorDetails?: string;
  createdAt: string;
  updatedAt: string;
}

const INITIAL_TASKS: Task[] = [
  {
    id: 1,
    name: "http.webhook.dispatch",
    status: "RUNNING",
    priority: 4,
    category: "Network & Integration",
    payloadJson: '{"orderId": "OR-4821", "event": "order.paid"}',
    retryCount: 0,
    maxRetries: 3,
    createdAt: "2026-08-19T19:00:00",
    updatedAt: "2026-08-19T19:02:00",
  },
  {
    id: 2,
    name: "file.document.render",
    status: "COMPLETED",
    priority: 3,
    category: "Data & File Processing",
    payloadJson: '{"documentId": "DOC-9921", "format": "pdf"}',
    retryCount: 1,
    maxRetries: 5,
    createdAt: "2026-08-19T18:45:00",
    updatedAt: "2026-08-19T18:46:00",
  },
];

export default function DashboardPage() {
  const router = useRouter();
  const [tasks, setTasks] = useState<Task[]>(INITIAL_TASKS);
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);
  const [isSubmitOpen, setIsSubmitOpen] = useState(false);

  // Task Creation Form State
  const [formCategory, setFormCategory] = useState("Network & Integration");
  const [formHandler, setFormHandler] = useState("http.webhook.dispatch");
  const [formPriority, setFormPriority] = useState(3);
  const [formMaxRetries, setFormMaxRetries] = useState(3);
  const [formPayload, setFormPayload] = useState(
    '{\n  "url": "https://hooks.internal.acme.dev/payload"\n}',
  );

  const handleCreateTask = (e: React.FormEvent) => {
    e.preventDefault();
    const newTask: Task = {
      id: Date.now(),
      name: formHandler,
      status: "QUEUED",
      priority: formPriority,
      category: formCategory,
      payloadJson: formPayload,
      retryCount: 0,
      maxRetries: formMaxRetries,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    setTasks([newTask, ...tasks]);
    setIsSubmitOpen(false);
  };

  const handleDeleteTask = (id: number) => {
    setTasks(tasks.filter((t) => t.id !== id));
    setSelectedTask(null);
  };

  const getStatusBadge = (status: Task["status"]) => {
    const styles = {
      RUNNING: "bg-cyan-950 text-cyan-400 border-cyan-800",
      COMPLETED: "bg-emerald-950 text-emerald-400 border-emerald-800",
      FAILED: "bg-rose-950 text-rose-400 border-rose-800",
      RETRY_WAITING: "bg-amber-950 text-amber-400 border-amber-800",
      QUEUED: "bg-slate-800 text-slate-300 border-slate-700",
    };
    return (
      <span
        className={`px-2 py-0.5 text-xs font-mono border rounded-full ${styles[status]}`}
      >
        ● {status}
      </span>
    );
  };

  return (
    <div className="min-h-screen bg-[#0b0f17] text-slate-100 flex font-sans antialiased">
      {/* Sidebar Navigation */}
      <aside className="w-64 border-r border-slate-800 p-4 flex flex-col justify-between hidden md:flex bg-[#0b0f17]">
        <div>
          <div className="flex items-center justify-between mb-8 px-2">
            <div className="flex items-center gap-3">
              <div className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse" />
              <span className="font-bold tracking-tight">TaskFlow</span>
            </div>
            <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-800 text-slate-400 font-mono">
              v0.4
            </span>
          </div>
          <nav className="space-y-1">
            <a
              href="#"
              className="flex items-center gap-2 px-3 py-2 rounded-lg bg-slate-800/60 text-emerald-400 text-sm font-medium"
            >
              Queue Management
            </a>
          </nav>
        </div>
        <div className="px-2 py-3 border-t border-slate-800/60 flex items-center justify-between">
          <div className="text-xs text-slate-500 font-mono">
            Java 21 virtual threads
            <br />
            PostgreSQL 16
          </div>
          <button
            onClick={() => router.push("/")}
            className="text-xs text-slate-400 hover:text-rose-400 font-mono"
            title="Sign out"
          >
            Logout
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className="flex-1 flex flex-col min-w-0">
        {/* Top bar */}
        <header className="h-16 border-b border-slate-800 px-6 flex items-center justify-between bg-[#0b0f17]/80 backdrop-blur-md sticky top-0 z-20">
          <div className="flex items-center gap-4">
            <h1 className="text-sm font-semibold text-slate-200">
              Queue Operations
            </h1>
            <span className="text-xs text-slate-400">
              · {tasks.length} active rows
            </span>
          </div>
          <button
            onClick={() => setIsSubmitOpen(true)}
            className="bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-medium px-3.5 py-2 rounded-lg transition-colors flex items-center gap-1.5 shadow-lg shadow-emerald-950/40"
          >
            + Submit task
          </button>
        </header>

        {/* Dashboard Content */}
        <div className="p-6 space-y-6 flex-1 overflow-auto">
          {/* Metrics Overview */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-[#111827] border border-slate-800/80 rounded-xl p-4">
              <span className="text-xs font-medium text-slate-400 uppercase tracking-wider">
                Active Workers
              </span>
              <div className="text-2xl font-bold font-mono mt-1 text-emerald-400">
                2 / 8
              </div>
            </div>
            <div className="bg-[#111827] border border-slate-800/80 rounded-xl p-4">
              <span className="text-xs font-medium text-slate-400 uppercase tracking-wider">
                Throughput
              </span>
              <div className="text-2xl font-bold font-mono mt-1 text-slate-200">
                25{" "}
                <span className="text-xs text-slate-400 font-normal">
                  tasks / min
                </span>
              </div>
            </div>
            <div className="bg-[#111827] border border-slate-800/80 rounded-xl p-4">
              <span className="text-xs font-medium text-slate-400 uppercase tracking-wider">
                Avg Handler Latency
              </span>
              <div className="text-2xl font-bold font-mono mt-1 text-cyan-400">
                9.2s
              </div>
            </div>
          </div>

          {/* Tasks Table */}
          <div className="bg-[#111827] border border-slate-800/80 rounded-xl overflow-hidden">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-800 text-[11px] font-mono text-slate-400 bg-slate-900/40">
                  <th className="py-3 px-4 font-medium">ID</th>
                  <th className="py-3 px-4 font-medium">HANDLER / NAME</th>
                  <th className="py-3 px-4 font-medium">CATEGORY</th>
                  <th className="py-3 px-4 font-medium">STATUS</th>
                  <th className="py-3 px-4 font-medium">PRI</th>
                  <th className="py-3 px-4 font-medium">ATTEMPTS</th>
                  <th className="py-3 px-4 font-medium">UPDATED</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 text-sm font-mono">
                {tasks.map((task) => (
                  <tr
                    key={task.id}
                    onClick={() => setSelectedTask(task)}
                    className="hover:bg-slate-800/40 cursor-pointer transition-colors"
                  >
                    <td className="py-3 px-4 text-xs text-slate-400">
                      #{task.id}
                    </td>
                    <td className="py-3 px-4 text-slate-200 font-medium text-xs">
                      {task.name}
                    </td>
                    <td className="py-3 px-4 text-xs text-slate-400">
                      {task.category}
                    </td>
                    <td className="py-3 px-4">{getStatusBadge(task.status)}</td>
                    <td className="py-3 px-4 text-xs text-slate-300">
                      {task.priority}
                    </td>
                    <td className="py-3 px-4 text-xs text-slate-400">
                      {task.retryCount}/{task.maxRetries}
                    </td>
                    <td className="py-3 px-4 text-xs text-slate-500">
                      {new Date(task.updatedAt).toLocaleTimeString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </main>

      {/* Task Details Slide-over Sidebar */}
      {selectedTask && (
        <div className="fixed inset-0 z-50 flex justify-end bg-black/60 backdrop-blur-xs animate-fadeIn">
          <div className="w-full max-w-md bg-[#111827] border-l border-slate-800 p-6 flex flex-col justify-between overflow-y-auto">
            <div>
              <div className="flex items-center justify-between pb-4 border-b border-slate-800 mb-6">
                <div>
                  <span className="text-xs font-mono text-slate-400">
                    Task Details Inspector
                  </span>
                  <h2 className="text-base font-bold text-slate-100 font-mono">
                    #{selectedTask.id}
                  </h2>
                </div>
                <button
                  onClick={() => setSelectedTask(null)}
                  className="text-slate-400 hover:text-slate-100 p-1"
                >
                  ✕
                </button>
              </div>

              <div className="space-y-4 text-sm">
                <div>
                  <span className="text-xs text-slate-400 block mb-1">
                    Status
                  </span>
                  {getStatusBadge(selectedTask.status)}
                </div>
                <div>
                  <span className="text-xs text-slate-400 block mb-1">
                    Handler Name
                  </span>
                  <span className="font-mono text-slate-200 text-xs bg-slate-900 px-2 py-1 rounded border border-slate-800 block">
                    {selectedTask.name}
                  </span>
                </div>
                <div>
                  <span className="text-xs text-slate-400 block mb-1">
                    Category
                  </span>
                  <span className="font-mono text-slate-300 text-xs">
                    {selectedTask.category}
                  </span>
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <span className="text-xs text-slate-400 block mb-1">
                      Priority
                    </span>
                    <span className="font-mono text-slate-200">
                      {selectedTask.priority}
                    </span>
                  </div>
                  <div>
                    <span className="text-xs text-slate-400 block mb-1">
                      Retries
                    </span>
                    <span className="font-mono text-slate-200">
                      {selectedTask.retryCount} / {selectedTask.maxRetries}
                    </span>
                  </div>
                </div>
                <div>
                  <span className="text-xs text-slate-400 block mb-1">
                    Payload JSON
                  </span>
                  <pre className="p-3 bg-slate-900 border border-slate-800 rounded-lg text-xs font-mono text-slate-300 overflow-x-auto">
                    {selectedTask.payloadJson}
                  </pre>
                </div>
              </div>
            </div>

            <div className="pt-6 border-t border-slate-800 mt-6 flex gap-3">
              <button
                onClick={() => handleDeleteTask(selectedTask.id)}
                className="w-full py-2.5 bg-rose-600/20 hover:bg-rose-600/30 text-rose-400 border border-rose-900/50 rounded-xl text-xs font-medium font-mono transition-colors"
              >
                Delete Task
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Submit Task Drawer */}
      {isSubmitOpen && (
        <div className="fixed inset-0 z-50 flex justify-end bg-black/60 backdrop-blur-xs">
          <div className="w-full max-w-md bg-[#111827] border-l border-slate-800 p-6 flex flex-col justify-between overflow-y-auto">
            <form onSubmit={handleCreateTask}>
              <div className="flex items-center justify-between pb-4 border-b border-slate-800 mb-6">
                <h2 className="text-base font-bold text-slate-100">
                  Submit New Task
                </h2>
                <button
                  type="button"
                  onClick={() => setIsSubmitOpen(false)}
                  className="text-slate-400 hover:text-slate-100"
                >
                  ✕
                </button>
              </div>

              <div className="space-y-4">
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">
                    Category
                  </label>
                  <div className="grid grid-cols-2 gap-2">
                    {[
                      "Network & Integration",
                      "Data & File Processing",
                      "Testing & Simulation",
                    ].map((cat) => (
                      <button
                        type="button"
                        key={cat}
                        onClick={() => setFormCategory(cat)}
                        className={`text-xs p-2 rounded-lg border text-left font-mono transition-colors ${
                          formCategory === cat
                            ? "bg-emerald-950/40 border-emerald-500 text-emerald-400"
                            : "bg-slate-900 border-slate-800 text-slate-400"
                        }`}
                      >
                        {cat}
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">
                    Handler
                  </label>
                  <select
                    value={formHandler}
                    onChange={(e) => setFormHandler(e.target.value)}
                    className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2.5 text-sm text-slate-200 font-mono focus:outline-none focus:border-emerald-500"
                  >
                    <option value="http.webhook.dispatch">
                      http.webhook.dispatch
                    </option>
                    <option value="file.document.render">
                      file.document.render
                    </option>
                    <option value="third.party.sync">third.party.sync</option>
                  </select>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-medium text-slate-400 mb-1">
                      Priority
                    </label>
                    <input
                      type="number"
                      min={1}
                      max={10}
                      value={formPriority}
                      onChange={(e) => setFormPriority(Number(e.target.value))}
                      className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2.5 text-sm text-slate-200 font-mono focus:outline-none focus:border-emerald-500"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-slate-400 mb-1">
                      Max Retries
                    </label>
                    <input
                      type="number"
                      min={0}
                      max={10}
                      value={formMaxRetries}
                      onChange={(e) =>
                        setFormMaxRetries(Number(e.target.value))
                      }
                      className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2.5 text-sm text-slate-200 font-mono focus:outline-none focus:border-emerald-500"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">
                    Payload (JSON)
                  </label>
                  <textarea
                    rows={5}
                    value={formPayload}
                    onChange={(e) => setFormPayload(e.target.value)}
                    className="w-full bg-slate-900 border border-slate-800 rounded-xl p-3 text-xs font-mono text-slate-200 focus:outline-none focus:border-emerald-500"
                  />
                </div>
              </div>

              <div className="pt-6 border-t border-slate-800 mt-8">
                <button
                  type="submit"
                  className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-sm font-medium transition-colors shadow-lg shadow-emerald-950/40"
                >
                  Dispatch to Queue
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
