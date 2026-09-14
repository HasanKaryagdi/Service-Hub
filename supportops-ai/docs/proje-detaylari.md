# SupportOps AI: Ne yaptık, nasıl çalışıyor?

## 1. Projenin amacı

SupportOps AI, production destek mühendisinin bir işlemi farklı sistemlerde tek tek araştırma ihtiyacını azaltan bir **Incident Investigation Platform** MVP'sidir. Transaction, payment, rebate, hesap hareketi, servis logu, Kafka olayı ve incident kayıtlarını ortak kimlikler üzerinden birleştirir.

Bu proje, SQL sorgulama, API troubleshooting, log inceleme, transaction araştırması, incident yönetimi ve root-cause analysis tecrübesini çalışan bir yazılım ürününde göstermeyi amaçlar. Yalnızca kayıt oluşturma ve listeleme yapmaz; bir işlemin başarısızlığını kaynak kayıtlarla açıklayan investigation akışı sunar.

Mevcut Service-Hub deposundaki Support Console korunur. Yeni platform `supportops-ai/` altında bağımsız backend, frontend, altyapı ve test dosyalarıyla bulunur. İki uygulamanın derleme kapsamları ayrıdır.

## 2. Destek mühendisinin uçtan uca akışı

1. Destek mühendisi hesabıyla giriş yapar.
2. Global Search ekranı mevcut veritabanı işlemlerini otomatik getirir. Son doğrulamada 516 kayıt vardı; sayfa başına 50 kayıt gösterilir. Bu sayı sabit değildir, simülasyon çalıştırıldıkça artar.
3. `transactionId`, `userId`, `reconciliationId`, `iban`, `requestId` veya `correlationId` ile arama yapabilir. Otomatik mod, kimliklerin prefix'ine güvenmek yerine altı alanı exact match ile kontrol eder.
4. Transaction ID'ye tıklayarak detay ekranını açar.
5. Timeline üzerinde farklı kaynaklardan gelen olayları zaman sırasıyla inceler. Bir event açıldığında kaynak kaydın detayları görünür.
6. Payment, Rebate, Logs, Kafka Events ve Database sekmeleriyle ilgili kayıtları karşılaştırır.
7. **Investigate**, ilgili veri ailesini toplar ve desteklenen hata örüntülerini değerlendirir. Sonuç bulguyu, heuristic confidence değerini ve kanıt kayıtlarını içerir.
8. **Create incident** ile konuyu kayda alır. Incident üzerinde incelemeye başlama, root cause ekleme, çözme, kapatma veya yeniden açma işlemleri yapılabilir.
9. Admin, Audit Trail ekranından investigation, query ve incident değişikliklerini takip eder.

## 3. Kullanılan teknoloji ve görevleri

| Teknoloji | Projedeki sorumluluk |
|---|---|
| Java 21 / Spring Boot 3.5 | Backend, business logic ve API |
| Spring Web / Validation | HTTP endpoint'leri, request doğrulama, hata cevapları |
| Spring Data JPA | Domain entity mapping ve kullanıcı repository'si |
| JDBC / PostgreSQL | Investigation projeksiyonları, ilişkili kayıtlar, timeline, dashboard, kontrollü SQL |
| Flyway | Schema kurulumu ve sürümlü veri düzeltmeleri |
| Spring Security / JWT / BCrypt | Kimlik doğrulama ve backend rol kontrolleri |
| Redis | Login denemelerine süreli limit |
| Kafka | Gerçek event yayınlama ve tüketme |
| React / TypeScript / Next.js | Operasyon paneli ve same-origin API proxy |
| Docker Compose | Yerel servis orkestrasyonu, healthcheck ve volume'lar |
| OpenTelemetry / Prometheus / Grafana / Tempo | Metrics ve trace altyapı yapılandırması |
| Ollama adaptörü | İsteğe bağlı yerel model ile SQL üretimi ve analiz anlatımı |
| JUnit / AssertJ / Testcontainers | Unit ve gerçek PostgreSQL entegrasyon testleri |
| GitHub Actions | Backend test ve frontend derleme kontrolleri |

## 4. Veri modeli

On temel entity tanımlandı:

| Entity | Anlamı |
|---|---|
| User | Demo kullanıcı, email, rol, BCrypt parola hash'i ve IBAN |
| Transaction | İşlem kimlikleri, kullanıcı, tutar, para birimi, tür, durum ve zamanlar |
| Payment | İşleme ait ödeme tutarı, kanal, hesap türü ve sonuç |
| AccountTransaction | Ledger/hesap hareketi kanıtı |
| Rebate | Orijinal işlem ve iade transaction bağlantısı, tutar, durum, hata nedeni |
| ServiceLog | Servis, seviye, mesaj, request/correlation ID, body, HTTP status ve timestamp |
| KafkaEvent | Topic, event key, transaction, payload, durum ve zaman |
| Incident | Başlık, açıklama, işlem, severity, durum, root cause ve çözüm zamanı |
| IncidentAnalysis | Kaydedilmiş analiz, provider, confidence ve evidence |
| AuditLog | Aktör, işlem, kaynak ve metadata |

Bunlara ek olarak `query_previews` kullanıcıya bağlı sorgu önizlemelerini, `outbox_events` henüz yayınlanmamış event'leri saklar. Foreign key, unique constraint ve arama indeksleri ilişkisel bütünlüğü destekler. Para tutarlarında floating point yerine PostgreSQL numeric / Java BigDecimal kullanılır.

## 5. Transaction family ve timeline

İade araştırması yalnızca orijinal transaction satırına bakmaz. Recursive CTE, orijinal işlem ile ilişkili rebate transaction'larını iki yönde takip eder. Böylece iadenin ayrı transaction ID'si olsa bile ortak investigation görünümüne dahil edilir.

Payment, rebate, transaction, account transaction, Kafka ve log kayıtları ortak bir timeline DTO'suna dönüştürülür. Zaman sırası `timestamp`, eşitlik durumunda UUID ile deterministik hale gelir. Veritabanında timestamptz, API'de ISO-8601/UTC, ekranda kullanıcının saat dilimi kullanılır.

Timeline'daki domain kayıtları, kayıt oluşturma zamanı ve güncel durum projeksiyonlarıdır; her durum değişikliğinin tam event-sourced geçmişi tutulmaz. Gerçek production bağlantısında immutable lifecycle event ingestion ve event zamanı/ingestion zamanı ayrımı ayrıca geliştirilmelidir.

Kaynak başına 1.000 kayıt sınırı vardır. Backend bir fazla kayıt okuyarak sınır aşımını tespit eder. Eksik kanıt setiyle otomatik analiz yapılmaz.

## 6. Investigation nasıl sonuç üretiyor?

Varsayılan analiz deterministik bir `EvidenceReasoner` üzerinden çalışır. Beş ana örüntü desteklenir:

| Demo işlem | Kontrol edilen bulgu | Analizin yorumu |
|---|---|---|
| `abc-123` | Payment başarılı, rebate FAILED, Wallet HTTP 400 | Refundable balance sıfır olduğu için iade reddedilmiş |
| `tx-002` | Başarılı ödeme ve FAILED Kafka kaydı | Event teslimatı, producer/outbox ve consumer durumları incelenmeli |
| `tx-003` | Wallet timeout / HTTP 504 | Downstream işlemin commit olup olmadığı bilinmiyor; yeniden denemeden önce reconciliation gerekir |
| `tx-004` | Duplicate rebate kanıtı | Önceki iade ve idempotency key kontrol edilmeli |
| `tx-005` | Başarılı payment için eşleşen account transaction bulunmaması | Ledger tüketimi/reconciliation araştırılmalı; kanıt yokluğu para kaybını tek başına ispatlamaz |

Analiz yalnızca bir metin döndürmez; bulgunun dayandığı kayıtları da döndürür ve kaydeder. Örneğin `%92`, modelin ölçülmüş doğruluk olasılığı değildir; ilgili kural için tanımlı heuristic confidence değeridir. Ekran bunu açıkça belirtir.

Ollama yapılandırıldığında desteklenen bulgu yerel model tarafından yeniden ifade edilebilir. Model çalışmazsa investigation, provider etiketinde durum belirtilerek kurallara döner. Gerçek model inference bu makinedeki doğrulama kapsamında çalıştırılmadı.

## 7. Natural Language to SQL ve güvenlik sınırı

AI Query ekranında kullanıcı doğal dille soru sorar. Model ayarlanmamışsa IBAN, demo transaction ID, başarısız kayıtlar ve 1–36 aylık süre gibi sınırlı şablonlar desteklenir. Ollama ayarlandığında model SQL önerir; öneri aynı güvenlik katmanından geçer.

Örnek soru: “Son 3 ay içerisinde bu IBAN'a yapılan işlemleri getir.” Sistem izinli analytics view'ına, IBAN eşitliği ve tarih filtresiyle bir SELECT hazırlar. SQL önce görünür; kullanıcı **Run query** demeden çalıştırılmaz.

Koruma katmanları:

1. **Kapalı gramer:** Sadece tanımlı tek SELECT biçimi kabul edilir. Blacklist ile birkaç kelime aramakla yetinilmez.
2. **View ve kolon allowlist:** Yalnızca analytics schema'sındaki izinli view/filtre sütunları kullanılır.
3. **Yasak yapılar:** INSERT, UPDATE, DELETE, DROP, ALTER, TRUNCATE, CREATE, GRANT, REVOKE; ayrıca CTE, JOIN, UNION, fonksiyon, comment, subquery, alias ve çoklu statement reddedilir.
4. **Ayrı kullanıcı:** Uygulamanın yazma yetkili bağlantısı yerine `supportops_reader` ile çalıştırılır. Kullanıcı ve parola hash tablolarına erişemez.
5. **Kaynak sınırları:** 3 saniye statement/query timeout, 1 saniye lock timeout, en fazla 200 satır ve bağlantı/socket timeout uygulanır.
6. **Preview sahipliği:** SQL 10 dakikalık preview olarak kullanıcıya bağlanır. İstemci çalıştırmada SQL yerine preview ID gönderir. Başka kullanıcı çalıştıramaz.
7. **İkinci doğrulama:** SQL execution öncesinde tekrar validate edilir.
8. **Audit:** Preview oluşturma, execution başlangıcı, başarı ve başarısızlık kaydedilir.

Bu yaklaşım SQL'in izin verilen işlemlerle sınırlı kalmasını sağlar. Ancak gramerden geçen SQL'in sorunun anlamını doğru karşılaması ayrı bir konudur; kullanıcı SQL'i ve sonucu değerlendirmelidir.

## 8. Authentication ve yetkiler

ADMIN tüm mevcut uygulama işlemlerine ve audit'e erişir. SUPPORT_ENGINEER görüntüleme, investigation, query ve incident yönetimi yapar. VIEWER yalnızca görüntüler. Yetkiler frontend düğmelerinin gizlenmesine bırakılmaz; Spring Security sunucuda zorunlu uygular.

JWT bir saat geçerlidir. Next.js proxy, JWT'yi HttpOnly / SameSite=Strict çerezde saklar; HTTPS origin'de Secure kullanır. Tarayıcı login response'unda token dönmez. Mutasyonlarda Origin, `APP_ORIGIN` ile eşleşmelidir. Hedef backend adresi sabit environment ayarından gelir.

Redis, beş dakikalık login deneme penceresi sağlar. Local proxy arkasında bu basit limit ortak proxy adresini kullanır. SSO/OIDC, refresh token, revocation, key rotation ve kullanıcı yönetim UI'ı bu MVP'de yoktur.

## 9. Incident yönetimi, benzerlik ve audit

Severity: LOW, MEDIUM, HIGH, CRITICAL. Durumlar: OPEN, INVESTIGATING, RESOLVED, CLOSED.

Incident kapatmak için önce çözüm akışı izlenir. RESOLVED durumu boş root cause kabul etmez. Yeniden açma, resolvedAt alanını temizler. Güncelleme DB row lock ile serileştirilir; incident audit kaydı aynı transaction'da yazılır.

Benzer incident bulma, son 500 kayıtta token Jaccard similarity kullanır. Sonuç yüzdesi metin örtüşmesidir; semantik embedding skoru değildir. `SimilarityProvider` arayüzü gelecekte vektör tabanlı adaptör eklemeyi mümkün kılar.

SQL audit kayıtları bağımsız transaction'da saklanır; SQL başarısızlığı nedeniyle audit'in de rollback olması önlenir. Metadata, aktör ve kaynak ID üzerinden inceleme yapılır. Tamper-proof/WORM audit storage bu sürümün kapsamında değildir.

## 10. Gerçek HTTP ve Kafka içeren mock ortam

Dört container: payment-service, transaction-service, wallet-service, rebate-service. Aynı Spring JAR farklı rollerle çalışır; ortak demo schema kullanılır.

Payment servisi gerçek HTTP ile transaction ve wallet servislerini çağırır. Rebate servisi wallet'tan HTTP 400 alabilir. Business kayıtları ile outbox aynı DB transaction'ında yazılır. Publisher, pending outbox kayıtlarını `FOR UPDATE SKIP LOCKED` ile alır, Kafka ack geldikten sonra published işaretler.

Ack ile DB commit arasında kesinti olursa aynı event tekrar gönderilebilir. Consumer event UUID üzerindeki unique constraint sayesinde tekrar gelen kaydı çoğaltmaz. Bu tasarım at-least-once delivery ve idempotent consumer yaklaşımıdır; exactly-once iddiası yoktur.

Altı çalıştırılabilir senaryo bulunur: success, rebate-zero, timeout, kafka-failed, duplicate, missing-account. Kafka başarılı akışta gerçektir; `kafka-failed` senaryosu broker'ı fiziksel olarak bozmak yerine başarısız teslimat kanıtı oluşturur. Timeout senaryosu gerçek bekleme yerine sentetik HTTP 504 üretir.

## 11. Seed, dashboard ve observability

İlk boş veritabanında 50 kullanıcı, 500 transaction, 450 payment, 50 rebate (30 başarısız), 20 incident, 1.031 log ve 100 Kafka event oluşturulur. Seed, advisory lock ve DB transaction ile bir kez uygulanır. Smoke testleri ek kayıt oluşturduğundan çalışan panelde sayılar artar.

Dashboard toplam/başarılı/başarısız transaction, açık ve critical incident, payment/rebate failure rate, son 24 saat hata grafiği, en çok hata veren servisler ve son incident'ları gerçek DB sorgularından gösterir.

Prometheus metrics endpoint'i ve histogram ayarı vardır. Observability profilinde OpenTelemetry collector → Tempo ve Grafana datasource/dashboard yapılandırması bulunur. Bu profil yerel doğrulama turunda açılmadı. Elasticsearch profili altyapı kurulumudur; aktif log store PostgreSQL, adapter sınırı `LogSource`'tur. Elasticsearch indeksleme veya arama adaptörü henüz uygulanmamıştır.

## 12. Testler ve geliştirme sırasında düzeltilen sorunlar

Son yerel test sonucu **42 başarılı test**, 0 hata, 0 atlama. Bunların 9'u gerçek PostgreSQL container'ında çalışır. Kapsam: SQL saldırı örnekleri, örüntü/evidence eşleşmesi, similarity, seed hacmi ve tutar bütünlüğü, arama, sıralı timeline, reader izinleri, preview sahipliği/süresi, RBAC, incident geçişleri ve 50 kayıtlık sayfalama.

Canlı HTTP smoke testi auth, search, investigation, SQL, incident/audit ve altı simülasyonu doğruladı. Tarayıcıda login, dashboard, event açma, investigation, SQL önizleme/çalıştırma ve Global Search pagination kontrol edildi.

Bulunan ve düzeltilen örnekler:

- Docker iç adresi ile dış adres farklı olduğundan login Origin kontrolü başarısız oluyordu; açık `APP_ORIGIN` ayarı ve regresyon testi eklendi.
- İlk demo seed'de ilişkili iade loglarının zamanı uyumsuzdu; yalnızca sentetik kayıtları hedefleyen Flyway migration eklendi.
- Rebate transaction tutarı ile rebate kaydının tutarı eşitlendi; veritabanı testi eklendi.
- Global Search başlangıçta yalnızca demo kartlarını gösteriyordu; gerçek DB listesi, toplam sayı, sayfalama, arama ve Show all eklendi.

## 13. GitHub entegrasyonu

Kaynaklar Service-Hub deposunda `supportops-ai/` altında tutulur. README, mimari, API sözleşmesi, doğrulama raporu ve bu açıklama birlikte sürümlenir. `.tools`, bağımlılıklar, build çıktıları ve kişisel `.env` dosyaları Git'e dahil edilmez. `.env.example` yalnızca açıkça belirtilmiş local demo değerlerini içerir.

GitHub Actions workflow'u `.github/workflows/supportops.yml` konumundadır. Backend job Java 21 ile testleri çalıştırır, PostgreSQL integration testlerinin atlanmasını hata sayar ve raporları artifact olarak saklar. Frontend job kilit dosyasından bağımlılık kurar, TypeScript kontrolü ve production build çalıştırır. Bu CI workflow'u deployment yapmaz.

## 14. Production'a geçmeden önce kalanlar

Bu ürün çalışan bir yerel portfolio MVP'sidir. Gerçek banka/ödeme platformuna bağlanmış veya production-ready olarak sertifikalandırılmış değildir. Bir sonraki aşamalar: gerçek source ingestion kontratları, PII redaction, tenant isolation/RLS, SSO, secret management, TLS, audit retention, Elasticsearch adapter, immutable event history, Kafka DLQ/redrive, HA, load/soak test, backup/restore ve model değerlendirme veri seti.

## 15. Portföyde nasıl anlatılır?

> Production destek mühendisliği deneyimimi, transaction yaşam döngüsünü veritabanı, log ve Kafka kanıtlarıyla birleştiren SupportOps AI platformuna dönüştürdüm. Java 21/Spring Boot backend ve Next.js arayüz geliştirdim; güvenli SQL preview/execution, rol bazlı erişim, incident workflow, audit, transactional outbox ve idempotent consumer kullandım. Sistem gerçek PostgreSQL testleri ve HTTP/Kafka simülasyonlarıyla doğrulandı. AI katmanını kontrolsüz database erişimi yerine izinli sorgular ve görünür kanıtlar etrafında tasarladım.

Kurulum için [README](../README.md), teknik kararlar için [mimari](architecture.md), endpoint'ler için [API sözleşmesi](api.md), sonuçların sınırları için [doğrulama raporu](verification.md) incelenebilir.
