import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = { title: "MoneyPay Service Hub", description: "MoneyPay servisleri ve gömülü modüller için entegrasyon çalışma alanı." };
export default function RootLayout({children}:{children:React.ReactNode}){return <html lang="tr"><body>{children}</body></html>}
