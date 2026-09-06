export function Loader() {
  return (
    <div className="flex items-center justify-center p-8 bg-[#0b0f17]">
      <div className="flex items-center gap-3 font-mono text-xs text-slate-400">
        <div className="w-4 h-4 rounded-full border-2 border-emerald-500/20 border-t-emerald-500 animate-spin" />
        <span>Loading...</span>
      </div>
    </div>
  );
}
