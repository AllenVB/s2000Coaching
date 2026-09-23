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

## Tecrübelerim (Antrenman / Beslenme / Kardiyo)

Herkese açık bir bölüm, ama içeriği yalnızca doğru düzenleme parolasını bilen kişi değiştirebilir. Parola
`EXPERIENCE_EDIT_TOKEN` ortam değişkeniyle ayarlanır; **yerel geliştirme dışında mutlaka gerçek bir değerle
ayarlayın** — ayarlanmazsa `s2000-dev-edit` varsayılanı kullanılır ve backend başlangıçta bunu loglarda uyarır.
Siteden "Düzenle"ye basıldığında parola bir kere sorulur ve tarayıcının `localStorage`'ında saklanır.

## Testler

```bash
cd backend && ./gradlew test   # unit + @WebMvcTest + Testcontainers (Docker gerekir)
cd frontend && npm run build   # tsc + vite build
```
