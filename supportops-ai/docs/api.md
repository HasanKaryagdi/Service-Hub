# HTTP API

Base: `http://localhost:8080/api`. JSON request/response. Login hariç `Authorization: Bearer <JWT>`. UI aynı endpoint'lere Next.js proxy üzerinden gider. Başarısız istekler problem-detail JSON döner (`status`, `detail`).

| Method | Yol | Girdi / Davranış |
|---|---|---|
| POST | `/auth/login` | `{email,password}` → `{token,name,role}` |
| GET | `/dashboard` | `{stats,errors,services,incidents}` |
| GET | `/transactions?page=0` | `{transactions,total,page,pageSize,hasNext}`; en yeni kayıtlar, sayfa başına 50 |
| GET | `/search?q=...&type=auto` | `{transactions,truncated,searchType}`; type: auto/transactionId/userId/reconciliationId/iban/requestId/correlationId |
| GET | `/transactions/{id}` | `{transaction,transactions,payments,rebates,account_transactions,service_logs,kafka_events,incidents,incident_analyses,timeline,truncated}` |
| POST | `/transactions/{id}/investigate` | `{id,transactionId,rootCause,confidence,provider,steps,evidence,createdAt}` |
| GET | `/incidents?page=0&q=...` | 50 incident projeksiyonu |
| POST | `/incidents` | `{title,description,transactionId,severity}` → `{incidentId}` |
| GET | `/incidents/{incidentId}` | Transaction aggregate + `incident`, `similar` |
| PATCH | `/incidents/{incidentId}` | `{status,rootCause}` |
| POST | `/ai-query/preview` | `{prompt}` → `{id,sql,expiresAt,provider}` |
| POST | `/ai-query/run` | `{previewId}` → `{rows,maxRows,possiblyTruncated}` |
| GET | `/logs?page=0&q=...` | Message filter; 50 log projeksiyonu |
| GET | `/audit?page=0&q=...` | Action filter; ADMIN; 50 audit projeksiyonu |

READ işlemleri tüm roller; mutasyonlar ADMIN/SUPPORT_ENGINEER. `/audit` yalnızca ADMIN.

Timeline event: `{id,type,service,status,timestamp,description,details}`. details kaynak kaydın explicit JDBC projeksiyonudur, timestamp ISO-8601'dir. Response verisi sentetik log body'leri içerir; gerçek source adaptöründen önce redaction eklenmelidir.

Incident geçişleri:

```text
OPEN → INVESTIGATING | RESOLVED
INVESTIGATING → OPEN | RESOLVED
RESOLVED → OPEN | CLOSED
CLOSED → OPEN
```

RESOLVED boş root cause kabul etmez. Reopen, resolvedAt alanını temizler. State update DB row lock ile serileştirilir; audit aynı transaction'da yazılır.

Simulator: `POST http://localhost:8081/internal/simulate`, `X-Simulator-Token` ve `{scenario}`. Sadece simulation profili.
