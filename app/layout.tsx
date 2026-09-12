import type { Metadata } from "next";
import "./globals.css";
import "./support.css";

export const metadata: Metadata = { title: "MoneyPay Support Console", description: "Servis sağlığı, kullanıcı tanısı ve support operasyonları için kontrol merkezi." };
export default function RootLayout({children}:{children:React.ReactNode}){return <html lang="tr"><body>{children}</body></html>}
