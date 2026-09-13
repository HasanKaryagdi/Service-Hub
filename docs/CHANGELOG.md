# Değişiklik kaydı

## 13 Eylül 2026

### Ortak sandbox modeli

- 100 sentetik kullanıcının kart, bakiye, hesap ve olay bilgileri ortak duruma bağlandı.
- Şikâyet kayıtları kullanıcı kimliğiyle ilişkilendirildi.
- Service Toolkit işlemlerinin Customer Intelligence ve Complaint Inbox ekranlarına anlık yansıması sağlandı.

### Service Toolkit

- cURL komut kütüphanesi ve düzenlenebilir çalışma alanı oluşturuldu.
- JSON body, `case_id`, `customer_id`, tutar, para birimi ve dry-run doğrulamaları eklendi.
- HTTP response, `before/after` karşılaştırması ve çalışma geçmişi eklendi.
- Bakiye düzeltme, iade, hak senkronizasyonu, hesap blokesi kaldırma ve kart durumu değiştirme senaryoları bağlandı.

### Customer Intelligence

- 100 sentetik kullanıcı ve ayrıntılı olay/log akışı oluşturuldu.
- Wallet, Benefits ve kartsız kullanıcı görünümleri eklendi.
- Aktif, pasif ve blokeli kart durumları ortak modele bağlandı.

### Service Observability

- Kullanıcı verisinden tamamen ayrıldı.
- Yalnızca sistem geneli günlük, haftalık ve aylık servis grafikleri bırakıldı.

### Complaint Inbox

- Servis araçları ve grafiklerden ayrıldı.
- Durum, sorumlu, zaman, kanal ve SLA bilgileri eklendi.
- İletilen, devam eden ve çözülen kayıtlar için filtreler eklendi.

### Marka ve arayüz

- Tüm eski marka çağrışımları kaldırılarak THE kimliği oluşturuldu.
- Uzay/taktik oyunlarından esinlenen Solar Orange, Obsidian ve Radar Teal paleti uygulandı.
- Açık/koyu tema desteği korundu.
- Geometrik THE amblemi, favicon ve AM profil rozeti eklendi.
- Masaüstü, dar menü ve mobil kırılımlar güncellendi.
