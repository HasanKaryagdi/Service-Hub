import type {Metadata} from 'next';
import './globals.css';
export const metadata:Metadata={title:'SupportOps AI · Incident intelligence',description:'Evidence-driven transaction investigation workspace'};
export default function Layout({children}:{children:React.ReactNode}){return <html lang="en"><body>{children}</body></html>;}
