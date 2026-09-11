"use client";

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useSession } from "@/lib/auth-client";
import { User, LogOut } from "lucide-react";
import { useRouter } from "next/navigation";

export function Header() {
  const { data } = useSession();
  const user = data?.user;
  const router = useRouter();

  return (
    <header className="border-b border-slate-800 px-6 h-16 flex items-center justify-between bg-[#0b0f17]">
      <div className="flex items-center gap-3">
        <div className="w-3 h-3 rounded-full bg-emerald-500" />
        <span className="font-bold tracking-tight text-base text-slate-100">
          TaskFlow
        </span>
        <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-800 text-slate-400 font-mono">
          v0.4
        </span>
      </div>

      <div className="flex items-center gap-6">
        <nav className="flex items-center gap-6 text-xs text-slate-400 font-mono">
          <span className="hover:text-slate-200 cursor-pointer">instances</span>
          <span>
            leader: <strong className="text-slate-200">node-a1</strong> · epoch
            17
          </span>
        </nav>

        <DropdownMenu>
          <DropdownMenuTrigger className="flex items-center justify-center h-9 w-9 rounded-lg bg-slate-900 border border-slate-800 text-slate-300 hover:text-slate-100 hover:border-slate-700 cursor-pointer focus:outline-none">
            <User className="h-4 w-4" />
          </DropdownMenuTrigger>
          <DropdownMenuContent
            className="w-56 bg-[#111827] border-slate-800 text-slate-200"
            align="end"
            forceMount
          >
            <DropdownMenuGroup>
              <DropdownMenuLabel className="font-normal">
                <div className="flex flex-col space-y-1">
                  <p className="text-sm font-medium leading-none text-slate-200">
                    {user?.name}
                  </p>
                  <p className="text-xs leading-none text-slate-400 font-mono">
                    {user?.email}
                  </p>
                </div>
              </DropdownMenuLabel>
            </DropdownMenuGroup>
            <DropdownMenuSeparator className="bg-slate-800" />
            <DropdownMenuGroup>
              <DropdownMenuItem
                className="focus:bg-slate-800 focus:text-slate-100 cursor-pointer text-xs font-mono"
                onClick={() => {
                  router.push("/dashboard/profile");
                }}
              >
                <User className="mr-2 h-4 w-4" />
                <span>Profile</span>
              </DropdownMenuItem>
            </DropdownMenuGroup>

            <DropdownMenuSeparator className="bg-slate-800" />
            <DropdownMenuGroup>
              <DropdownMenuItem
                className="focus:bg-slate-800 focus:text-slate-100 cursor-pointer text-xs font-mono"
                onClick={() => {
                  router.push("/dashboard/api-keys");
                }}
              >
                <User className="mr-2 h-4 w-4" />
                <span>API Keys</span>
              </DropdownMenuItem>
            </DropdownMenuGroup>

            <DropdownMenuGroup>
              <DropdownMenuItem
                className="focus:bg-slate-800 focus:text-slate-100 cursor-pointer text-xs font-mono"
                onClick={() => {
                  router.push("/dashboard/workflows");
                }}
              >
                <User className="mr-2 h-4 w-4" />
                <span>Workflows</span>
              </DropdownMenuItem>
            </DropdownMenuGroup>
            <DropdownMenuSeparator className="bg-slate-800" />
            <DropdownMenuGroup>
              <DropdownMenuItem className="focus:bg-rose-950/50 focus:text-rose-400 cursor-pointer text-xs font-mono text-rose-400">
                <LogOut className="mr-2 h-4 w-4" />
                <span>Logout</span>
              </DropdownMenuItem>
            </DropdownMenuGroup>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}
