"use client";

import { useMemo, useState } from "react";
import { Activity, Bell, BookOpen, CheckCircle2, ChevronDown, CircleAlert, Clock3, Command, Headphones, Inbox, LayoutDashboard, MessageSquareText, MoreHorizontal, Search, Settings, ShieldCheck, UsersRound } from "lucide-react";

const tickets = [
  { id: "#4821", title: "Ödeme ekranında 3D Secure dönüşü bekliyor", company: "Mavi Masa", person: "Deniz Aksoy", priority: "Kritik", age: "12 dk", status: "Yeni", channel: "Canlı destek" },
  { id: "#4819", title: "Toplu kullanıcı aktarımında sütun eşleşmiyor", company: "Aralık Lojistik", person: "Bora Şen", priority: "Yüksek", age: "28 dk", status: "İnceleniyor", channel: "E-posta" },
  { id: "#4816", title: "Mobil uygulamada bildirim izni yeniden soruluyor", company: "Hane Finans", person: "Selin Güler", priority: "Normal", age: "46 dk", status: "Müşteri yanıtı", channel: "Portal" },
  { id: "#4812", title: "Rapor dışa aktarımında tarih biçimi hatalı", company: "Duru Eğitim", person: "Ece Ural", priority: "Normal", age: "1 sa 08 dk", status: "İnceleniyor", channel: "E-posta" },
  { id: "#4808", title: "Yeni rol için fatura görüntüleme yetkisi", company: "Mono Studio", person: "Emre Kılıç", priority: "Düşük", age: "1 sa 34 dk", status: "Planlandı", channel: "Portal" },
];

const filters = ["Tümü", "Yeni", "İnceleniyor", "Müşteri yanıtı"];

export default function Home() {
  const [filter, setFilter] = useState("Tümü");
  const [query, setQuery] = useState("");
  const visible = useMemo(() => tickets.filter(t => (filter === "Tümü" || t.status === filter) && `${t.title} ${t.company} ${t.id}`.toLocaleLowerCase("tr").includes(query.toLocaleLowerCase("tr"))), [filter, query]);

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <a className="logo" href="#top" aria-label="Northline ana sayfa"><span><Command size={18}/></span><strong>Northline</strong></a>
        <nav aria-label="Ana menü">
          <p>ÇALIŞMA ALANI</p>
          <a className="active" href="#top"><LayoutDashboard/>Genel bakış</a>
          <a href="#tickets"><Inbox/>Ticket kuyruğu <b>12</b></a>
          <a href="#team"><UsersRound/>Ekip</a>
          <a href="#reports"><Activity/>Raporlar</a>
          <p>KAYNAKLAR</p>
          <a href="#knowledge"><BookOpen/>Bilgi bankası</a>
          <a href="#settings"><Settings/>Ayarlar</a>
        </nav>
        <div className="sidebar-foot"><div className="status-dot"/><div><strong>Sistemler çalışıyor</strong><span>Son kontrol 2 dk önce</span></div></div>
      </aside>

      <section className="workspace" id="top">
        <header className="topbar">
          <div className="search"><Search size={17}/><input value={query} onChange={e=>setQuery(e.target.value)} aria-label="Ticket ara" placeholder="Ticket, müşteri veya konu ara"/><kbd>⌘ K</kbd></div>
          <button className="icon-button" aria-label="Bildirimler"><Bell size={18}/><i/></button>
          <button className="profile"><span>EA</span><div><strong>Elif Arslan</strong><small>Vardiya lideri</small></div><ChevronDown size={15}/></button>
        </header>

        <div className="content">
          <section className="overview">
            <div className="intro-copy"><span className="eyebrow">ÇARŞAMBA · SABAH VARDİYASI</span><h1>Günaydın Elif.<br/><em>Kuyruk kontrol altında.</em></h1><p>Öncelik isteyen iki kayıt var. Ödeme hatası SLA sınırına 18 dakika uzaklıkta.</p></div>
            <div className="shift-card"><div><span className="pulse"/>CANLI VARDİYA</div><strong>08.00—16.00</strong><p>6 uzman aktif · 1 molada</p><button>Vardiyayı yönet <ChevronDown size={15}/></button></div>
          </section>

          <section className="metrics" aria-label="Operasyon özeti">
            <article><span>AÇIK TICKET</span><strong>24</strong><small><b>−6</b> dünden beri</small></article>
            <article className="metric-accent"><span>İLK YANIT</span><strong>8<em> dk</em></strong><small>Hedefin 2 dk altında</small></article>
            <article><span>BUGÜN ÇÖZÜLEN</span><strong>37</strong><small><b>+12%</b> günlük akış</small></article>
            <article><span>MEMNUNİYET</span><strong>4.8<em>/5</em></strong><small>Son 48 değerlendirme</small></article>
          </section>

          <section className="work-grid">
            <div className="ticket-panel" id="tickets">
              <div className="panel-head"><div><span className="eyebrow">AKTİF KUYRUK</span><h2>Yanıt bekleyenler</h2></div><button className="new-ticket"><MessageSquareText size={17}/>Yeni ticket</button></div>
              <div className="filter-row" role="group" aria-label="Ticket filtreleri">{filters.map(item=><button key={item} onClick={()=>setFilter(item)} className={filter===item?"selected":""}>{item}{item==="Yeni"&&<span>3</span>}</button>)}</div>
              <div className="ticket-list">
                {visible.map((t,idx)=><article className="ticket" key={t.id}>
                  <div className={`priority p-${t.priority.toLocaleLowerCase("tr").replace("ü","u")}`} title={`${t.priority} öncelik`}/>
                  <div className="ticket-main"><div className="ticket-title"><span>{t.id}</span><h3>{t.title}</h3></div><p>{t.company} · {t.channel}</p></div>
                  <div className="owner"><span>{t.person.split(" ").map(n=>n[0]).join("")}</span><div><strong>{t.person}</strong><small>{t.status}</small></div></div>
                  <time><Clock3 size={14}/>{t.age}</time><button aria-label={`${t.id} seçenekleri`}><MoreHorizontal size={18}/></button>
                </article>)}
                {visible.length===0&&<div className="empty">Bu filtreyle eşleşen ticket bulunamadı.</div>}
              </div>
              <a className="all-link" href="#all">Tüm kuyruğu aç <span>→</span></a>
            </div>

            <aside className="ops-rail">
              <article className="sla-card"><div className="rail-title"><span><ShieldCheck size={18}/>SLA takibi</span><a href="#sla">Detay</a></div><div className="sla-ring"><div><strong>92%</strong><small>hedefte</small></div></div><div className="sla-legend"><p><i className="good"/>22 kayıt güvende</p><p><i className="warn"/>2 kayıt riskli</p></div></article>
              <article className="incident"><div><CircleAlert size={20}/><span><small>AKTİF OLAY</small><strong>Bildirim servisinde gecikme</strong></span></div><p>Mobil push mesajları ortalama 4 dakika gecikiyor. Müşteri iletişim metni hazır.</p><a href="#incident">Olay kaydını aç →</a></article>
              <article className="team-card" id="team"><div className="rail-title"><span><Headphones size={18}/>Ekip yükü</span><a href="#team">Tümü</a></div>{[["BK","Bora Kaya",4],["SD","Sude Demir",3],["MO","Mert Okan",3]].map(x=><div className="agent" key={String(x[0])}><span>{x[0]}</span><p><strong>{x[1]}</strong><small>{x[2]} aktif ticket</small></p><div className="load"><i style={{width:`${Number(x[2])*20}%`}}/></div></div>)}</article>
            </aside>
          </section>
        </div>
      </section>
    </main>
  );
}
