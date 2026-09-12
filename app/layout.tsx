import type { Metadata } from "next";
import "./globals.css";
import "./support.css";
import "./branding.css";

export const metadata: Metadata = { title: "THE Support Console", description: "Servis sağlığı, kullanıcı tanısı ve destek operasyonları için bağımsız kontrol merkezi.", icons: { icon: "/favicon.svg", shortcut: "/favicon.svg" } };
export default function RootLayout({children}:{children:React.ReactNode}){return <html lang="tr"><body>{children}</body></html>}
