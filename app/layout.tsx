import type { Metadata } from "next";
import "./globals.css";
import "./support.css";
import "./api-inspector.css";
import "./workspaces.css";
import "./curl-catalog.css";
import "./service-minimal.css";
import "./observability.css";
import "./complaints.css";
import "./service-runner.css";
import "./view-isolation.css";
import "./branding.css";

export const metadata: Metadata = { title: "THE Support Console", description: "A unified control surface for service health, customer diagnostics, and support operations.", icons: { icon: "/favicon.svg", shortcut: "/favicon.svg" } };
export default function RootLayout({children}:{children:React.ReactNode}){return <html lang="tr"><body>{children}</body></html>}
