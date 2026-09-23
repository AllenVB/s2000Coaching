# S2000 Coaching

Online fitness & beslenme koçluğu tanıtım sitesi ve sipariş akışı. `stitch_fitcoach_premium_platform/` ve
`stitch_fitcoach_premium_platform (1)/` altındaki iki Stitch tasarımının birleştirilmiş halidir (bkz. bu
klasörlerdeki `DESIGN.md`).

## Yapı

- `backend/` — Spring Boot 4 (Java 21, Gradle, PostgreSQL, Liquibase). Paket ve sipariş REST API'si.
- `frontend/` — React 19 + Vite + TypeScript + Tailwind v4. Landing sitesi ve sepet/checkout akışı.
- `docker-compose.yml` — yerel geliştirme için yalnızca PostgreSQL.

## Yerel geliştirme

```bash
# 1) Veritabanı
docker compose up -d postgres

# 2) Backend (varsayılan port 8080; meşgulse SERVER_PORT ile değiştirin)
cd backend
./gradlew bootRun

# 3) Frontend
cd frontend
cp .env.example .env   # VITE_API_BASE_URL'i backend portunuza göre düzenleyin
npm install
npm run dev
```

Frontend `http://localhost:5173`, backend varsayılan olarak `http://localhost:8080` üzerinde çalışır.

## Ödeme durumu

Şu an gerçek bir ödeme sağlayıcısı (iyzico/Stripe) entegre değil. Siparişler `MockPaymentGateway` üzerinden
anında `MOCK_PAID` durumuyla oluşturulur; checkout formu kart bilgisi toplamaz. Gerçek sağlayıcı eklenmek
istendiğinde `backend/src/main/java/com/s2000coaching/payment/PaymentGateway.java` arayüzünün yeni bir
implementasyonu yazılması yeterlidir — controller/service/frontend akışı değişmez.

## Tecrübelerim (dinamik kategoriler)

Herkese açık bir bölüm. Sağdaki **Düzenleme Modu** ile kategoriler (Antrenman, Beslenme, Kardiyo … dilediğiniz
kadar) ve her kategori altındaki başlık + açıklama şeklindeki tecrübe kartları eklenip düzenlenip silinebilir.
**Uyarı:** İçerik düzenleme herkese açıktır — siteye giren herkes kategori/tecrübe ekleyip silebilir. Kısıtlamak
isterseniz bu endpoint'lere kimlik doğrulama eklenmesi gerekir.

## Testler

```bash
cd backend && ./gradlew test   # unit + @WebMvcTest + Testcontainers (Docker gerekir)
cd frontend && npm run build   # tsc + vite build
```
