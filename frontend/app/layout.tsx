import type { Metadata } from "next";
import { Montserrat } from "next/font/google";
import "./globals.css";
import Providers from "./providers";

const montserrat = Montserrat({
  variable: "--font-montserrat",
  subsets: ["latin"],
  weight: ["100", "200", "300", "400", "500", "600", "700", "800", "900"],
});

export const metadata: Metadata = {
  title: "Task Flow",
  description: "Task Flow",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" className="dark">
      <body
        className={`${montserrat.variable} antialiased min-h-screen bg-[#0a0c10] text-[#e8e8e8]`}
      >
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
