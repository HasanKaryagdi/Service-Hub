# Service Integration Hub

Servisleri, API bağlantılarını ve gömülü modülleri tek bir çalışma alanında birleştirmek için hazırlanmış modern bir entegrasyon dashboard prototipi.

[Canlı demoyu aç](https://northline-support-ops.hsnkrgd.chatgpt.site)

![Service Integration Hub dashboard](docs/screenshots/service-integration-hub.png)

## Öne çıkanlar

- Açık ve koyu tema; tercih tarayıcıda saklanır
- Servis kataloğu ve hızlı arama
- REST, GraphQL ve webhook bağlantı yüzeyleri
- Iframe, mikro-frontend ve özel bileşenler için gömülü modül alanı
- Masaüstü, tablet ve mobil için responsive düzen
- WCAG AA odaklı kontrast ve klavye erişilebilirliği
- Lucide outline ikon ailesi

## Tasarım sistemi

- Ana renk: `#F36B21`
- Nötr: `#17181B`
- Vurgu: `#2D59CF`
- Başlık: Georgia
- Gövde: Segoe UI
- Boşluk sistemi: `8 / 16 / 24 / 32 / 48 / 64 / 96`

## Teknolojiler

- React 19
- TypeScript
- Vinext / Vite
- Tailwind CSS
- Lucide React
- Cloudflare uyumlu worker çıktısı

## Yerel geliştirme

Node.js `22.13.0` veya üzeri gerekir.

```bash
npm install
npm run dev
```

Üretim kontrolü:

```bash
npm run build
```

## Proje yapısı

```text
app/
  page.tsx        Dashboard arayüzü ve tema davranışı
  globals.css     Tasarım sistemi ve responsive stiller
public/           Marka ve banner varlıkları
worker/           Cloudflare uyumlu uygulama girişi
docs/screenshots/ README görselleri
```

## Marka notu

Veyra Support, sentetik veriler kullanan bağımsız bir destek operasyonu prototipidir. Herhangi bir gerçek şirket, ürün veya müşteri verisiyle bağlantılı değildir.

## Lisans

Bu repository portföy ve prototipleme amacıyla paylaşılmıştır.
