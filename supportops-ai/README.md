# SupportOps AI

**[Ne yaptık? Ayrıntılı Türkçe proje açıklaması](docs/proje-detaylari.md)** — kullanım senaryosu, veri modeli, mimari, SQL güvenliği, Kafka, AI sınırları, testler ve portföy anlatımı.

Transaction, payment ve rebate hatalarını veritabanı kayıtları, servis logları ve Kafka olaylarıyla birlikte araştıran yerel incident investigation platformu.

Java 21 / Spring Boot 3.5 · PostgreSQL · Redis · Kafka · Next.js / React / TypeScript. Ana uygulama modüler monolith; dört ayrı container çalışan servis, sentetik production akışını simüle eder. Tüm demo verileri sentetiktir.

## Başlatma

Docker Desktop Linux container motorunu açın. Proje klasöründe:

```sh
docker compose --profile simulation up -d --build
```

- Panel: http://localhost:3000
- API health: http://localhost:8080/actuator/health
- Demo payment simulator: http://localhost:8081/internal/simulate

İlk çalıştırma image/bağımlılık indirmeleri nedeniyle zaman alır. Core stack ve dört JVM için Docker'a yaklaşık 8 GB bellek ayırın; observability/search profilleri ek kaynak ister. Klasördeki `.env.example` değerleri yalnızca yerel demo içindir. İsterseniz `.env` olarak kopyalayıp değerleri değiştirin. PostgreSQL rol parolaları volume ilk oluşturulduğunda uygulanır; sonradan `.env` değiştirmek mevcut rol parolasını değiştirmez.

| Hesap | Rol | Yetki |
|---|---|---|
| `admin@supportops.local` | ADMIN | Tüm mevcut uygulama işlemleri ve audit |
| `engineer@supportops.local` | SUPPORT_ENGINEER | Görüntüleme, investigation, query, incident yönetimi |
| `viewer@supportops.local` | VIEWER | Görüntüleme |

Varsayılan demo parolası: `SupportOps-local-2026!`. Özelleştirilmiş kurulumda `DEMO_PASSWORD` kullanılır. JWT 1 saat geçerlidir; frontend JWT'yi HttpOnly/SameSite çerezde tutar. API, Bearer JWT de kabul eder.

Servisleri durdurmak için `docker compose --profile simulation down`. Bu komut veritabanı volume'larını korur.

## İlk investigation

1. Engineer hesabıyla giriş yapın.
2. Global Search ekranında `abc-123` arayın veya demo senaryosunu seçin.
3. Transaction detayında Timeline event'ini açın; request/response, HTTP status ve correlation bilgilerini inceleyin.
4. **Investigate** ile desteklenen bulgu ve kanıt listesini oluşturun.
5. **Create incident** ile kaydı açın, açıklama/severity belirleyin.
6. Incident detayında root cause ekleyip **Resolve** seçin.
7. Admin hesabında Audit Trail üzerinden işlemleri kontrol edin.

| Kimlik | Senaryo |
|---|---|
| `abc-123` | Payment başarılı, rebate başarısız, refundable balance zero |
| `tx-002` | Payment başarılı, Kafka event başarısız |
| `tx-003` | Payment başarısız, Wallet timeout; downstream sonuç kesin değil |
| `tx-004` | Duplicate rebate request |
| `tx-005` | Başarılı payment için account transaction eksik |

Alternatif aramalar: `usr-001`, `rec-0001`, `req-001`, `corr-001`, `TR720001002136978165000001`. Auto search, opaque ID'leri prefix tahmini yerine altı namespace'te exact match ile arar. Sonuçtan transaction açılınca ilgili payment, rebate, log, Kafka ve incident kayıtları birlikte gelir.

Seed: 50 kullanıcı, 500 transaction, 450 payment, 50 rebate (30 FAILED), 20 incident, 1.031 log, 100 Kafka event. Seed advisory lock ile korunur; boş veritabanında bir defa, tek transaction içinde oluşturulur.

## AI ve güvenli SQL

API anahtarı olmadan platform çalışır. Varsayılan investigation, beş desteklenen örüntüyü kanıttan çıkaran deterministik kurallardır. Confidence değerleri test edilmiş olasılıklar değil, açıklanan heuristic skorlarıdır. Varsayılan NL-to-SQL sınırlı şablonları kullanır; genel amaçlı bir dil modeli değildir. UI ve API provider bilgisini gösterir.

Yerel Ollama kullanmak için modeli ayrı olarak indirin, `.env` içine ekleyip API'yi yeniden oluşturun:

```dotenv
OLLAMA_URL=http://host.docker.internal:11434
OLLAMA_MODEL=llama3.2
```

```sh
docker compose up -d --force-recreate api
```

Bu modda model NL sorudan SQL üretir; aynı validator'dan geçmeyen SQL reddedilir. Investigation anlatıcısı yalnızca kural katmanının desteklediği bulguyu yeniden ifade eder; DB erişimi veya araç çalıştırma yetkisi yoktur. Model erişilemiyorsa investigation açık provider etiketiyle kurallara döner. SQL üretiminde sessiz fallback yapılmaz. Modelin ürettiği anlatı mühendis tarafından kaynak kayıtlarla değerlendirilmelidir.

SQL akışı:

1. `POST /api/ai-query/preview`: SQL üret, doğrula, kullanıcıya bağlı 10 dakikalık preview kaydet ve audit yaz.
2. Kullanıcı SQL'i inceler; frontend sadece preview ID'yi gönderir.
3. `POST /api/ai-query/run`: preview sahipliği/süresi ve SQL yeniden doğrulanır.
4. Ayrı `supportops_reader` rolüyle analytics view'ları okunur; 3 saniye timeout, 1 saniye lock timeout, 200 satır üst sınırı uygulanır.
5. Başlangıç, başarı ve başarısızlık audit kayıtları bağımsız transaction ile saklanır.

Validator bir blacklist değildir: kapalı bir gramer kabul eder. Tek `SELECT *`, izinli analytics view, izinli sütunda literal eşitlik, 1–36 aylık tarih filtresi, sabit sıralama ve limit. JOIN, UNION, CTE, fonksiyon, comment, alias, subquery, çoklu statement, DDL/DML ve lock işlemleri reddedilir. `app_users` ve parola hash'leri reader erişimi dışında kalır. Preview SQL'i değiştirmek için yeni bir önizleme oluşturulur.

Örnek: `Son 3 ay içerisinde TR720001002136978165000001 IBANına yapılan işlemleri getir.`

## Dağıtık demo akışı

```sh
curl -X POST http://localhost:8081/internal/simulate \
  -H 'Content-Type: application/json' \
  -H 'X-Simulator-Token: local-simulator-secret' \
  -d '{"scenario":"rebate-zero"}'
```

Senaryolar: `success`, `rebate-zero`, `timeout`, `kafka-failed`, `duplicate`, `missing-account`. Dönen `transactionId` ile arama yapın. Windows PowerShell'de `curl.exe` kullanabilir veya JSON body ile `Invoke-RestMethod` çağırabilirsiniz.

Payment → Transaction → Wallet çağrıları gerçek HTTP'dir. Rebate servisi Wallet'tan gerçek HTTP 400 alır. `timeout` senaryosu bekletme yerine sentetik HTTP 504 üretir. `kafka-failed` senaryosu gerçek broker'ı bozmaz; başarısız teslimat kanıtı üretir. Başarılı event akışı gerçek Kafka kullanır: business kayıtlarıyla atomik outbox → publisher → broker → idempotent consumer. Broker kapalıysa outbox bekler; yeniden başlatıldığında tekrar denenir. Consumer event UUID üzerinde tekilleştirilir.

Demo servisleri aynı JAR'dan farklı rollerle çalışır ve ortak schema kullanır. Bağımsız servis veritabanları, gerçek ödeme işlemleri ve dağıtık saga kapsam dışıdır. Simulator sadece local porta açılır ve ayrı token gerektirir.

## Observability ve search profilleri

Trace export için `.env` dosyasında `TRACING_ENABLED=true` ayarlayın. Metrics export her zaman açıktır.

```sh
docker compose --profile simulation --profile observability up -d
```

- Grafana: http://localhost:3001 (`admin` / `local-grafana-password`)
- Prometheus: http://localhost:9090
- Hazır dashboard: **SupportOps · API health** — request rate, p95 duration, JVM heap.
- OpenTelemetry → collector → Tempo ile HTTP trace'leri. Grafana Explore/Tempo'dan izlenir.

```sh
docker compose --profile search up -d elasticsearch
```

Elasticsearch profili yalnızca altyapıyı kurar. Aktif log store PostgreSQL'dir; `LogSource` arayüzü Elasticsearch adaptörü için sınır oluşturur. Elasticsearch indexing/search ve vektör embedding entegrasyonu bu sürümde uygulanmamıştır. Benzer incident'lar son 500 kayıt üzerinde token Jaccard similarity ile bulunur; `SimilarityProvider` embedding adaptörüyle değiştirilebilir.

## Geliştirme ve doğrulama

```sh
# Java 21 + Maven 3.9; Docker açık olmalı
cd backend
mvn test

# Node 22 + pnpm 11.19
cd ../frontend
pnpm install --ignore-workspace --frozen-lockfile --ignore-scripts
pnpm typecheck
pnpm build
pnpm dev

# Çalışan Compose üzerinde Python 3 ile HTTP smoke testi
cd ..
python scripts/smoke.py
```

Frontend local dev, `API_URL` verilmezse `http://localhost:8080` kullanır. Docker içinde API adresi Compose tarafından ayarlanır.

Tarayıcı adresi `APP_ORIGIN` ile birebir eşleşmelidir (varsayılan `http://localhost:3000`). Farklı host/port kullanırsanız bu değişkeni güncelleyin; CSRF kontrolü proxy'nin iç container adresine veya güvenilmeyen forwarded host başlığına dayanmaz.

Testler SQL bypass örnekleri, evidence eşleştirme, similarity, gerçek PostgreSQL/Flyway/JPA başlangıcı, seed sayıları, beş senaryo, search/timeline, reader yetkileri, preview sahipliği/expiry, RBAC ve incident geçişlerini kapsar. Testcontainers Docker bulamazsa integration testlerini **skip** eder; bu durum uçtan uca başarı sayılmamalıdır. Container image build'inde Docker-in-Docker gerekmediği için yalnızca unit testler çalışır; CI ayrı integration aşamasında Docker kullanır.

Detaylar: [Mimari](docs/architecture.md), [API sözleşmesi](docs/api.md), [Doğrulama raporu](docs/verification.md).

## Sınırlar ve sonraki aşama

Bu sürüm, çalışan bir yerel portfolio MVP'sidir; doğrudan gerçek production verisine bağlanmak için hazır olduğu iddia edilmez. Tenant izolasyonu, PII redaction, SSO, refresh/revocation, key rotation, kullanıcı yönetim UI'ı, gerçek telemetry/log ingestion, HA, load test, retention ve embedding search ayrıca geliştirilmelidir. Kafka consumer retry varsayılan Spring Kafka error handler'ını kullanır; explicit DLQ/redrive yönetimi sonraki aşamadır. Listelemeler 50 kayıtlık sayfalar, arama 100 sonuç, timeline kaynak başına 1.000 kayıtla sınırlıdır. Eksik kanıtla otomatik analiz reddedilir.

Stack uyumluluk referansları: [Spring Boot 3.5 gereksinimleri](https://docs.spring.io/spring-boot/3.5/system-requirements.html), [Next.js kurulum dokümanı](https://nextjs.org/docs/app/getting-started/installation).
