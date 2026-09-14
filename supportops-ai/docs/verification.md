# Doğrulama raporu

14 Eylül 2026 · Yerel Windows / Docker Desktop Linux containers.

## Tamamlanan kontroller

| Kontrol | Sonuç |
|---|---|
| Java 21 backend derlemesi | Başarılı |
| JUnit/AssertJ testleri | 42 geçti; 0 hata, 0 atlanan |
| Gerçek PostgreSQL Testcontainers entegrasyonu | 9 test geçti; toplam 42'ye dahil |
| Global Search kayıt listesi | 516 mevcut işlem, 50 kayıtlık sayfalama, identifier araması ve Show all tarayıcıda doğrulandı |
| Next.js production build ve TypeScript | Başarılı; local ve Docker build |
| Docker Compose config | Geçerli |
| Core + simulation profili | API, frontend, PostgreSQL, Redis, Kafka ve dört simulator çalıştı |
| Canlı HTTP smoke | Auth, search, beş investigation, RBAC, SQL preview/run/ownership, incident resolution ve audit geçti |
| Browser proxy regresyonu | İzinli Origin kabul edildi, yabancı Origin 403, token HttpOnly çerezde; response body'de token yok |
| Dağıtık senaryolar | success, rebate-zero, timeout, kafka-failed, duplicate, missing-account geçti |
| Gerçek Kafka tüketimi | Simulator outbox olayları Kafka üzerinden investigation kayıtlarına ulaştı |
| Tarayıcı kontrolü | Login → dashboard → transaction → genişletilmiş HTTP 400 logu → investigation → SQL preview → query results doğrulandı |
| Demo tutar bütünlüğü | Rebate transaction tutarı ile rebate kaydı eşitliği PostgreSQL testinde doğrulandı |

Tarayıcı testinde Docker iç adresiyle Origin karşılaştırması giriş isteğini reddediyordu. `APP_ORIGIN` ile açık dış adres doğrulaması eklendi ve HTTP regresyon kontrolüyle doğrulandı. Eski seed kurulumlarındaki log zamanları ve rebate tutarları volume silmeden Flyway V2/V3 migration'larıyla düzeltilir.

## Kanıt dosyaları ve tekrar çalıştırma

- Test kaynakları: `backend/src/test/`.
- Canlı kontrat testi: `scripts/smoke.py`.
- Bu bilgisayardaki geçici çıktılar: `.tools/tests-final.log`, `.tools/smoke-final.log`, `.tools/compose-build-final.log`.
- Tekrar: `mvn -f backend/pom.xml test`, ardından çalışan Compose üzerinde `python scripts/smoke.py`.

Smoke testi sentetik incident, analysis, audit ve simulator kayıtları ekler. Dolayısıyla çalışan veritabanı sayıları seed minimumlarının üzerine çıkar; veri temizlenmemiştir.

## Doğrulanmayan / sonraki aşama

- Ollama adaptörü derlendi; bu makinede model kurulup gerçek LLM inference çalıştırılmadı. Varsayılan provider `evidence-rules` / `constrained-template` olarak test edildi.
- Observability ve Elasticsearch profilleri yapılandırıldı; collector/Tempo/Grafana ve Elasticsearch runtime entegrasyonu bu doğrulama turunda açılmadı. Aktif log store PostgreSQL'dir.
- Load/soak, broker failover, explicit DLQ/redrive, SSO, tenant isolation, PII redaction ve gerçek production kaynakları doğrulanmadı.

Bu rapor çalışan yerel MVP'nin doğrulamasıdır; production readiness sertifikasyonu değildir.
