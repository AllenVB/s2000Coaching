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
| `/uye` | Üye alanı | E-posta koduyla giriş yapan üyeler |
| `/yonetim` | Yönetim paneli | E-posta + şifre ile giriş yapan koç hesapları |

**Üye alanı** (masaüstünde sol menü, telefonda alt sekme çubuğu): `/uye` bugün · `/uye/antrenman` (ve
`/uye/antrenman/:gunId` gün detayı) · `/uye/beslenme` · `/uye/olcumler` · `/uye/mesajlar` · `/uye/profil`.

**Yönetim**: `/yonetim` panel (onay bekleyenler, ölçüm girmeyenler, son siparişler) · `/yonetim/uyeler`
liste (arama + durum filtresi) · `/yonetim/uyeler/yeni` · `/yonetim/uyeler/:id` üye detayı ·
`/yonetim/sablonlar` plan şablonları · `/yonetim/hareketler` hareket katalogu · `/yonetim/siparisler` ·
`/yonetim/ayarlar`.

Üye detayı sekmelidir ve her sekme ayrı bir adrestir (`…/:id`, `…/:id/antrenman`, `…/:id/beslenme`,
`…/:id/olcumler`, `…/:id/notlar`); koçun bir üye için yaptığı her iş bu sayfada toplanır. **Notlar**
yalnızca koça görünür — bu veriyi okuyan hiçbir üye endpoint'i yoktur.

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


## Antrenman planları

Akış: **hareket katalogu → plan şablonu → üyeye kopyala → üye yapar ve kaydeder.**

`/yonetim/hareketler` tüm planların çektiği ortak katalogdur. Bir planda kullanılan hareket silinemez
(silinirse o planlar sessizce boşalır ve kayıtlı setler sahipsiz kalırdı).

`/yonetim/sablonlar` sayfasında program bir kez yazılır (günler + her günde set/tekrar/hedef kilo/dinlenme).
Üye detayındaki **Antrenman** sekmesinden şablon seçilip üyeye atanır; atama **derin kopyadır**, yani
üyenin planında yapılan değişiklik şablonu ya da başka üyenin kopyasını etkilemez. Bir üyenin birden çok
planı olabilir ama yalnızca biri **aktiftir**; üye alanında görünen odur.

Üye `/uye/antrenman` sayfasında günleri görür, güne girip setlerini kaydeder. Gün detayı tek sayfadır
(salonda hareket arası gezinmek istemezsiniz) ve alanlar **geçen seansın değerleriyle** ön-doludur; ilk
kez yapılıyorsa hedef kilo yazar. Aynı gün ve tarihe tekrar kaydetmek o seansı düzeltir, ikinci kayıt
açmaz. Üyenin "Bugün" sayfası en uzun süredir yapılmamış günü **Sıradaki Antrenman** olarak önerir.

Koç, üye detayındaki Antrenman sekmesinin altında üyenin son seanslarını görür.

## Beslenme planları

Antrenmanla aynı kalıp: `/yonetim/sablonlar` sayfasının **Beslenme** sekmesinde şablon yazılır (öğünler ve
her öğünde besin + miktar + kalori/makro), üye detayındaki **Beslenme** sekmesinden üyeye kopyalanır. Kopya
yine bağımsızdır ve üyenin yalnızca bir planı aktiftir.

Plan düzeyinde **makro hedefi** (kalori/protein/karbonhidrat/yağ) tutulur; arayüz bunun yanında öğünlerin
fiili toplamını da gösterir — aradaki fark planın henüz tamamlanmadığını söyleyen şeydir. Makro girmek
zorunlu değil: besinleri makro yazmadan da reçete edebilirsiniz.

Üye `/uye/beslenme` sayfasında günlük hedeflerini ve öğünlerini görür.

## Mesajlaşma

Üye başına tek bir yazışma dizisi, iki taraflı: üye `/uye/mesajlar`, koç üye detayındaki **Mesajlar**
sekmesi. Diziyi açmak **karşı tarafın** mesajlarını okundu işaretler — okunmamış rozetlerini ve koçun
cevap kuyruğunu sürükleyen şey budur. Gönderenin kendi balonunda mesaj henüz okunmadıysa "okunmadı"
yazar.

Yönetim paneli, **son mesajı okunmamış** (yani cevap bekleyen) üyeleri ayrı bir blokta listeler; koç
diziyi açtığı anda üye bu kuyruktan düşer.

## Sipariş ↔ üyelik

Checkout ad, e-posta ve telefon topluyor ama üye oluşturmuyordu; aynı kişiyi bir de yönetim panelinden
elle girmek gerekiyordu. Artık:

- `/yonetim/siparisler` her siparişi **Üye / Üye değil** olarak işaretler. "Üyeye Dönüştür" düğmesi üye
  formunu siparişteki ad, e-posta ve telefonla açar (`/yonetim/uyeler/yeni?siparis=S2K-…`); koç yalnızca
  boy, kilo ve spor geçmişini ekler.
- Üye oluşturulduğunda **aynı e-posta ile verilmiş tüm bağlanmamış siparişler** otomatik olarak o üyeye
  bağlanır, böylece sipariş geçmişi ilk günden görünür (üye detayı → Profil → Siparişler).
- Üye silindiğinde siparişler **silinmez, yalnızca üyeden kopar** — sipariş mali bir kayıttır ve üyelikten
  uzun yaşar.

## Ölçüm takibi

Üye `/uye/olcumler` sayfasından haftalık check-in girer: tarih ve kilo zorunlu, bel/göğüs/kalça/kol/bacak
ve not isteğe bağlı. **Gün başına tek kayıt** tutulur — aynı tarihe tekrar girilirse o günün satırı
güncellenir, ikinci satır oluşmaz. Üye kendi kayıtlarını silebilir; koç üye detayındaki **Ölçümler**
sekmesinden grafiği ve tabloyu görür (koç ölçüm giremez, yalnızca okur ve siler). Grafik tek seferde tek
ölçüyü çizer; üstteki düğmelerden hangi ölçünün gösterileceği seçilir.

Yönetim paneli, **10 gündür ölçüm girmemiş aktif üyeleri** ayrı bir blokta listeler — "bugün kimi
aramalıyım" sorusunun cevabı.

### KVKK — sağlık verisi

Ölçümler KVKK'da **özel nitelikli kişisel veri** sayılır, bu yüzden checkout'taki KVKK onayı bunları
kapsamaz. Üye, ölçüm girmeden önce `/uye/olcumler` sayfasındaki ayrı onayı vermek zorundadır; onay
verilene kadar sunucu hiçbir ölçüm yazmaz (`member.health_data_consent_at`). Onay verilmemesi üyeliği
etkilemez, yalnızca ölçüm takibi kapalı kalır.

> **Yapılacak:** `frontend/src/components/member/HealthDataConsent.tsx` içindeki aydınlatma metni
> **taslaktır**. Yayına çıkmadan önce kendi hukuk danışmanınızın onayladığı metinle değiştirin.

Fotoğraflı takip (önce/sonra karşılaştırması) henüz yok: nesne depolama (S3 vb.) kararı gerektirdiği için
kapsam dışı bırakıldı.

## Yapılandırma

| Ortam değişkeni | Varsayılan | Açıklama |
| --- | --- | --- |
| `ADMIN_EMAIL` | — | İlk koç hesabının e-postası. Tanımlı değilse hesap oluşturulmaz ve panele girilemez. |
| `ADMIN_PASSWORD` | — | İlk koç hesabının şifresi. Yalnızca bcrypt özeti saklanır. |
| `ADMIN_NAME` | `Koç` | Panelde görünen ad. |
| `ADMIN_SESSION_TTL` | `12h` | Yönetim oturumunun geçerlilik süresi. |
| `ADMIN_LOCKOUT_DURATION` | `15m` | 5 hatalı şifreden sonra hesabın kilitli kalma süresi. |
| `VERIFICATION_CODE_TTL` | `15m` | Onay/giriş kodunun geçerlilik süresi. |
| `MEMBER_SESSION_TTL` | `14d` | Üye oturumunun geçerlilik süresi. |
| `MAIL_FROM` | `no-reply@s2000coaching.com` | Gönderen adresi. |
| `SPRING_MAIL_HOST` (+ `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`) | — | SMTP sunucusu. |

**Önemli:** `SPRING_MAIL_HOST` tanımlı değilse e-posta gönderilmez; kodlar yalnızca uygulama loguna
`WARN` seviyesinde yazılır (`LoggingVerificationMailSender`). Bu yalnızca yerel geliştirme içindir —
prodüksiyonda SMTP ayarları mutlaka verilmelidir.

### Yönetim girişi

Panel artık paylaşılan bir anahtarla değil, gerçek koç hesaplarıyla korunuyor:

- **İlk hesap** uygulama ilk açılışta `ADMIN_EMAIL` / `ADMIN_PASSWORD` değişkenlerinden oluşturulur
  (`CoachAccountSeeder`). Veritabanında hesap varsa bir daha çalışmaz — yani değişkenleri sonradan
  değiştirmek, koçun kendi değiştirdiği şifreyi sıfırlamaz.
- **Şifreler** yalnızca bcrypt özeti olarak saklanır; düz metin hiçbir yere yazılmaz.
- **Oturum** `POST /api/admin/auth/login` ile açılır, `Authorization: Bearer <jeton>` başlığıyla
  taşınır ve `ADMIN_SESSION_TTL` sonunda düşer. Jetonun kendisi değil SHA-256 özeti saklanır, bu
  yüzden çalınan bir oturum (`POST /api/admin/auth/logout`) tek tek iptal edilebilir.
- **Yanlış e-posta ile yanlış şifre aynı cevabı verir** (`E-posta veya şifre hatalı.`), böylece uç
  nokta hangi hesapların var olduğunu söylemez.
- **5 hatalı denemeden sonra** hesap `ADMIN_LOCKOUT_DURATION` süresince şifre kabul etmez.

> **Sunucu dil ayarı:** JVM ortam değişkenlerini `sun.jnu.encoding` ile çözer. Sunucunun dil ayarı
> UTF-8 değilse `ADMIN_NAME=Süleyman Koç` uygulamaya bozuk ulaşır. Seeder bunu fark edip uyarı
> basar ve adı bozuk kaydetmez, ama doğru ad için `LANG=C.UTF-8` tanımlanmalıdır.

Birden fazla koç gerektiğinde `coach_account` tablosuna satır eklemek yeterli; panelde hesap
yönetimi ekranı henüz yok.

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

Aynı üçü her push ve pull request'te GitHub Actions'ta da koşar (`.github/workflows/ci.yml`).
Testcontainers testleri runner'ın Docker'ıyla çalışır; backend testi düşerse HTML raporu iş
çıktısı olarak yüklenir.
