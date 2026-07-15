"use client";

import { useEffect, useState } from "react";
import { AppWindow, Blocks, BookOpen, ChevronRight, CircleHelp, Command, CreditCard, Database, ExternalLink, LayoutGrid, Moon, PlugZap, Search, Settings, ShieldCheck, Sun, WalletCards, Webhook } from "lucide-react";

const services = [
  { name:"Ödeme servisleri", detail:"POS, link ve tahsilat bileşenleri", icon:CreditCard, tone:"orange" },
  { name:"Kimlik & güvenlik", detail:"Oturum, yetki ve doğrulama servisleri", icon:ShieldCheck, tone:"blue" },
  { name:"Veri servisleri", detail:"Raporlama ve veri kaynağı bağlantıları", icon:Database, tone:"mint" },
  { name:"Webhook merkezi", detail:"Olay dinleyicileri ve geri çağrılar", icon:Webhook, tone:"rose" },
];

export default function Home(){
  const [dark,setDark]=useState(false);
  const [query,setQuery]=useState("");
  useEffect(()=>{const stored=localStorage.getItem("mp-theme");const value=stored?stored==="dark":window.matchMedia("(prefers-color-scheme: dark)").matches;setDark(value);document.documentElement.dataset.theme=value?"dark":"light"},[]);
  const toggle=()=>setDark(v=>{const next=!v;document.documentElement.dataset.theme=next?"dark":"light";localStorage.setItem("mp-theme",next?"dark":"light");return next});
  const shown=services.filter(s=>`${s.name} ${s.detail}`.toLocaleLowerCase("tr").includes(query.toLocaleLowerCase("tr")));
  return <main className="portal">
    <aside className="sidebar">
      <a className="brand" href="#top"><img src="/moneypay-logo.svg" alt="MoneyPay"/><span>Service Hub</span></a>
      <nav aria-label="Ana menü">
        <p>ÇALIŞMA ALANI</p>
        <a className="active" href="#top"><LayoutGrid/>Genel bakış</a>
        <a href="#services"><Blocks/>Servis kataloğu</a>
        <a href="#embed"><AppWindow/>Gömülü ekranlar</a>
        <a href="#connections"><PlugZap/>Bağlantılar</a>
        <p>YÖNETİM</p>
        <a href="#docs"><BookOpen/>Dokümantasyon</a>
        <a href="#settings"><Settings/>Ayarlar</a>
      </nav>
      <div className="sidebar-note"><Command/><div><strong>Entegrasyon alanı</strong><span>Servis eklemeye hazır</span></div></div>
    </aside>

    <section className="workspace" id="top">
      <header className="topbar">
        <div className="search"><Search/><input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Servis veya modül ara" aria-label="Servis ara"/></div>
        <button className="theme-toggle" onClick={toggle} aria-label={dark?"Açık moda geç":"Koyu moda geç"}>{dark?<Sun/>:<Moon/>}<span>{dark?"Açık mod":"Koyu mod"}</span></button>
        <button className="help"><CircleHelp/></button>
        <div className="profile"><span>MP</span><div><strong>Platform Ekibi</strong><small>Yönetici</small></div></div>
      </header>

      <div className="content">
        <section className="money-banner" aria-label="MoneyPay tanıtım bannerı"><div className="banner-overlay"><span>MoneyPay Service Hub</span><h1>Servisleri tek bir<br/>çalışma alanında birleştirin.</h1><p>Hazır entegrasyon yüzeyleriyle mevcut ürünlerinizi bağlayın, gömülü modülleri ekiplerinize açın.</p><a href="#services">Servis alanlarını incele <ChevronRight/></a></div></section>

        <section className="section-head" id="services"><div><span className="eyebrow">SERVİS MİMARİSİ</span><h2>Bağlantı noktaları hazır.</h2><p>Buradaki yüzeyler gerçek servisler bağlandığında içerik üretir; örnek iş, ticket veya sahte operasyon verisi göstermez.</p></div><button><PlugZap/>Yeni servis alanı</button></section>

        <section className="service-grid">
          {shown.map(({name,detail,icon:Icon,tone})=><article className="service-card" key={name}><div className={`service-icon ${tone}`}><Icon/></div><div className="service-copy"><h3>{name}</h3><p>{detail}</p></div><span className="waiting">Kurulum bekliyor</span><button aria-label={`${name} yapılandır`}><ChevronRight/></button></article>)}
          {shown.length===0&&<div className="empty">Bu aramayla eşleşen servis alanı yok.</div>}
        </section>

        <section className="integration-layout" id="embed">
          <div className="embed-canvas"><div className="canvas-head"><div><span className="eyebrow">GÖMÜLÜ MODÜL ALANI</span><h2>Servisiniz için boş tuval.</h2></div><span className="ready"><i/>Hazır</span></div><div className="drop-zone"><div className="drop-icon"><AppWindow/></div><h3>Gömülecek modül burada çalışacak</h3><p>Iframe, mikro-frontend veya özel bileşen bağlantısı için bu alan ayrıldı.</p><button><Blocks/>Modül yuvasını yapılandır</button></div></div>
          <aside className="connection-panel" id="connections"><div className="panel-title"><span><WalletCards/>Bağlantı özeti</span><button><Settings/></button></div><div className="connection-state"><span>0</span><p><strong>Aktif bağlantı yok</strong><small>İlk servisi bağladığınızda burada görünecek.</small></p></div><div className="connector"><div><Webhook/><span><strong>API bağlantısı</strong><small>REST veya GraphQL</small></span></div><ChevronRight/></div><div className="connector"><div><ExternalLink/><span><strong>Harici uygulama</strong><small>Güvenli gömülü görünüm</small></span></div><ChevronRight/></div><a href="#docs">Entegrasyon rehberini aç <ChevronRight/></a></aside>
        </section>
      </div>
    </section>
  </main>
}
