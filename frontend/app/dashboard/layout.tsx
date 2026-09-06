import React from "react";
import { Header } from "@/components/Header";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen bg-[#0b0f17] text-slate-100 flex flex-col font-sans antialiased">
      <Header />
      <main className="flex-1 flex flex-col min-w-0 p-6">{children}</main>
    </div>
  );
}
