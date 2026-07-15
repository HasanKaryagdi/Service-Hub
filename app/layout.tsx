import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = { title: "Northline Support — Operasyon", description: "Yazılım destek ekibi için ticket ve SLA operasyon dashboard'u." };
export default function RootLayout({children}:{children:React.ReactNode}){return <html lang="tr"><body>{children}</body></html>}
