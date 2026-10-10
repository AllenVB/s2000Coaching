# S2000 Coaching

Online fitness & beslenme koçluğu tanıtım sitesi, sipariş akışı, üye alanı ve yönetim paneli. `stitch_fitcoach_premium_platform/` ve
`stitch_fitcoach_premium_platform (1)/` altındaki iki Stitch tasarımının birleştirilmiş halidir (bkz. bu
klasörlerdeki `DESIGN.md`).

## Yapı

- `backend/` — Spring Boot 4 (Java 21, Gradle, PostgreSQL, Liquibase). Paket, sipariş ve üyelik REST API'si.
- `frontend/` — React 19 + Vite + TypeScript + Tailwind v4 + React Router. Landing sitesi, üye alanı ve
  yönetim paneli.
- `docker-compose.yml` — yerel geliştirme için yalnızca PostgreSQL.

### Sayfalar

| Yol | Sayfa | Erişim |
| --- | --- | --- |
| `/` | Tanıtım sitesi, paketler ve checkout | Herkese açık |
| `/uye` | Üye alanı: profil ve (yakında) planlar | E-posta koduyla giriş yapan üyeler |
| `/yonetim` | Yönetim: üye ekleme/çıkarma ayarları | Yönetim anahtarı (`ADMIN_TOKEN`) |

Yönlendirme `BrowserRouter` ile yapılır; prodüksiyonda statik sunucunun bilinmeyen yolları
`index.html`'e düşürmesi (SPA fallback) gerekir, aksi halde `/uye` ve `/yonetim` doğrudan açıldığında
404 döner.

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
Yerel yönetim anahtarı varsayılan olarak `s2000-dev-admin`'dir (`/yonetim` sayfasında sorulur).

## Üyelik akışı

Üyeler kendileri kayıt olmaz; koç `/yonetim` sayfasından oluşturur.

1. **Yönetim → Yeni Üye Ekle.** E-posta, ad soyad, telefon, boy, kilo ve spor geçmişi girilir. Form
   gönderildiğinde önce **onay penceresi** çıkar ("Bu kullanıcıyı oluşturmak istediğinize emin misiniz?");
   istek ancak onaylanınca gönderilir. Silme de aynı şekilde onay ister.
2. **Onay kodu e-postası.** Üye `PENDING_VERIFICATION` durumunda oluşturulur ve adresine 6 haneli,
   15 dakika geçerli tek kullanımlık bir kod gönderilir. Kodun yalnızca SHA-256 özeti saklanır.
3. **Onay.** Kod doğrulandığında üyelik tamamlanır (`ACTIVE`). Kodu üye `/uye` sayfasında kendisi
   girebilir (bu aynı anda girişini de yapar) veya koç yönetim sayfasındaki satırdan girebilir.
   Beş hatalı denemeden sonra kod kilitlenir; "Kodu Tekrar Gönder" ile yenisi üretilir.
4. **Giriş.** Şifre yoktur: üye `/uye` sayfasında e-postasını girer, gelen kodla oturum açar. Oturum
   jetonu opak bir değerdir, yalnızca özeti saklanır ve varsayılan 14 gün geçerlidir.

Üyelerin antrenman/beslenme planları sonraki aşamada eklenecek; üye panelinde bu alanlar şimdilik
"Yakında" kartları olarak duruyor.

## Yapılandırma

| Ortam değişkeni | Varsayılan | Açıklama |
| --- | --- | --- |
| `ADMIN_TOKEN` | `s2000-dev-admin` | `/api/admin/**` uçlarının `X-Admin-Token` başlığıyla karşılaştırdığı anahtar. **Prodüksiyonda mutlaka değiştirin.** |
| `VERIFICATION_CODE_TTL` | `15m` | Onay/giriş kodunun geçerlilik süresi. |
| `MEMBER_SESSION_TTL` | `14d` | Üye oturumunun geçerlilik süresi. |
| `MAIL_FROM` | `no-reply@s2000coaching.com` | Gönderen adresi. |
| `SPRING_MAIL_HOST` (+ `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`) | — | SMTP sunucusu. |

**Önemli:** `SPRING_MAIL_HOST` tanımlı değilse e-posta gönderilmez; kodlar yalnızca uygulama loguna
`WARN` seviyesinde yazılır (`LoggingVerificationMailSender`). Bu yalnızca yerel geliştirme içindir —
prodüksiyonda SMTP ayarları mutlaka verilmelidir.

**Yetkilendirme notu:** Yönetim paneli tek bir paylaşılan anahtarla korunur, gerçek kullanıcı hesabı
değildir. Koç dışında kişilerin erişmesi gerekirse Spring Security tabanlı gerçek kimlik doğrulamaya
geçilmelidir.

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
cd frontend && npm run lint    # oxlint
```
