# Yayına alma rehberi

Bu rehber siteyi kendi bilgisayarından çıkarıp internette, gerçek üyelerin kullanacağı bir sunucuda
çalıştırmak içindir. Sıfırdan başlayana göre yazıldı; her adımda ne yaptığını ve neden yaptığını
söyler. Baştan sona **1–2 saat** sürer (ilk derleme dahil).

> **Önemli:** Bu sitede üyelerin kilo/ölçü/fotoğraf bilgileri ve ödeme kayıtları duruyor. Bunlar
> kişisel veri, hatta sağlık verisi. Aşağıdaki "Canlıya almadan önce" bölümündeki test adımlarını
> atlamayın; en çok sorun çıkaran şeyler (e-posta gitmemesi, yedeğin geri yüklenememesi) ancak
> denenince anlaşılıyor.

## Ne kuruluyor

Üç konteyner, tek bir `docker compose` komutuyla:

| Parça | Ne yapar | Dışarıdan erişilir mi |
| --- | --- | --- |
| `web` (Caddy) | Siteyi sunar, HTTPS sertifikasını kendisi alır ve yeniler, `/api` isteklerini backend'e iletir | **Evet** (80 ve 443) |
| `backend` | Uygulamanın kendisi | Hayır |
| `postgres` | Veritabanı | Hayır |

Veritabanı ve fotoğraflar Docker "volume"larında durur; konteyner yeniden kurulsa da kaybolmaz.
Sitenin adresi ve API tek alan adından sunulduğu için tarayıcıda CORS ayarı gerekmez.

Boştayken ölçülen bellek: backend ≈ 430 MB, veritabanı ≈ 60 MB, web ≈ 30 MB. **2 vCPU / 4 GB RAM / 40 GB
disk** bir sunucu 100 üye için rahat yeter; 2 GB RAM'e kadar inebilir ama derleme sırasında zorlanır.

## 0. Başlamadan önce karar verilecekler

1. **Alan adı.** Bir alan adı satın alın (örn. `s2000coaching.com`). DNS yönetim paneline erişiminiz olmalı.
2. **Sunucu.** Ubuntu 24.04 LTS çalıştıran bir VPS (sanal sunucu). Herhangi bir sağlayıcı olur.
   **Sunucunun hangi ülkede olduğu önemli:** üyelerin sağlık verisi KVKK'da "özel nitelikli kişisel
   veri"dir ve yurt dışına aktarımın ayrı koşulları vardır. Hangi sağlayıcı ve hangi ülkenin size
   uygun olduğunu, aydınlatma ve rıza metinlerinizle birlikte avukatınıza sorun. (Bu rehber hukuki
   danışmanlık değildir.)
3. **E-posta (SMTP) hesabı.** Üyeler **şifresiz**, e-postayla gelen kodla giriş yapıyor. E-posta
   gitmezse hiçbir üye giriş yapamaz. Bir e-posta sağlayıcısından SMTP bilgilerini (sunucu, port,
   kullanıcı, parola) alın.
4. **Ödeme bilgileri.** Havale için IBAN ve hesap sahibi; kartla ödeme için iyzico hesabı (isteğe bağlı).
5. **Metinler.** KVKK aydınlatma/rıza ve başarı hikâyesi rıza metinleri **taslaktır** (README'de de
   yazıyor). Canlıya çıkmadan önce kendi avukatınızın onayladığı metinle değiştirin.

## 1. Sunucuyu hazırlayın

Sunucuya SSH ile bağlanın (`ssh root@SUNUCU_IP`). Önce güncelleyin ve kendinize yetkisiz bir
kullanıcı açmak yerine en azından şunları yapın:

```bash
apt update && apt upgrade -y
apt install -y ufw unattended-upgrades git

# Yalnızca SSH, HTTP ve HTTPS dışarıya açık olsun.
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable

# Güvenlik güncellemeleri kendiliğinden kurulsun.
dpkg-reconfigure -plow unattended-upgrades
```

Ek olarak SSH'ı **parola yerine anahtarla** kullanın ve `root` ile parolalı girişi kapatın (sağlayıcınızın
SSH anahtarı ekleme ekranından anahtarınızı ekleyin). Sunucu internete açıldığı dakikadan itibaren
parola deneyen botlar gelir; bu en önemli tek önlemdir.

Docker'ı kurun:

```bash
curl -fsSL https://get.docker.com | sh
docker --version && docker compose version
```

## 2. Alan adını sunucuya yönlendirin

DNS panelinizde bir **A kaydı** açın: `ornek.com` → sunucunun IP adresi (varsa `www` için de).
Yayılması birkaç dakikadan birkaç saate kadar sürebilir. Kontrol: `ping ornek.com` sunucu IP'nizi
göstermeli. Bu olmadan HTTPS sertifikası alınamaz.

## 3. Kodu sunucuya alın

```bash
git clone https://github.com/AllenVB/s2000Coaching.git /opt/s2000
cd /opt/s2000
```

(Depo özel ise GitHub'da bir *deploy key* ya da kişisel erişim anahtarı oluşturmanız gerekir.)

## 4. Ayarları yazın

```bash
cp deploy/.env.example deploy/.env
chmod 600 deploy/.env
nano deploy/.env
```

Her satırın ne olduğu dosyanın içinde yazıyor. En önemlileri:

| Ayar | Ne girilir |
| --- | --- |
| `SITE_ADDRESS` | `ornek.com` (başında `https://` olmadan) |
| `PUBLIC_URL` | `https://ornek.com` (e-postalardaki bağlantılar bununla kurulur) |
| `DB_PASSWORD` | Uzun, rastgele: `openssl rand -base64 24` çıktısı. **Bir kez yazılır**, aşağıdaki nota bakın |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | İlk koç hesabı. Yalnızca boş veritabanında, bir kez kullanılır |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | Sağlayıcınızın SMTP bilgileri. Boş bırakılırsa yığın **hiç başlamaz** (bilerek) |
| `PAYMENT_IBAN*` | Havale bilgileri. Üçü de boşsa havale kapalı olur |
| `PAYMENT_IYZICO_*` | Kartla ödeme. Anahtarlar boşsa kart kapalı olur. İlk önce **sandbox** adresi ve sandbox anahtarlarıyla kalın |

Notlar:
- **Parolaları bir sohbete, e-postaya ya da depoya yapıştırmayın.** `deploy/.env` git tarafından
  zaten yok sayılır.
- `DB_PASSWORD`, veritabanı ilk oluşturulurken kullanılır. Sonradan `.env`'de değiştirmek veritabanının
  parolasını **değiştirmez**; uygulama bağlanamaz hale gelir. Değiştirmeniz gerekirse önce
  `docker compose -f deploy/docker-compose.yml exec postgres psql -U s2000coaching -c "ALTER USER s2000coaching PASSWORD 'yeni'"`
  çalıştırın, sonra `.env`'yi güncelleyin.
- Saat dilimi `APP_TIME_ZONE` (varsayılan `Europe/Istanbul`): "bugün"ün hangi gün sayıldığını belirler.
  Sunucu UTC'de olsa bile üyenin gece 01:00'deki antrenmanı doğru güne yazılır. Değiştirirseniz site
  yeniden derlenmeli (aşağıdaki komut zaten derler).

## 5. Başlatın

```bash
docker compose -f deploy/docker-compose.yml up -d --build
```

İlk seferde 5–10 dakika sürer (Java ve npm paketleri iner, site derlenir). Sonra:

```bash
docker compose -f deploy/docker-compose.yml ps        # üçü de "Up" ve backend "healthy" olmalı
docker compose -f deploy/docker-compose.yml logs -f backend   # "Started BackendApplication" görünmeli (Ctrl+C ile çıkın)
```

Tarayıcıdan `https://ornek.com` adresini açın. Sertifikayı Caddy ilk istekte kendisi alır; bir iki
dakika gecikebilir. Açılmazsa en alttaki **Sorun giderme** bölümüne bakın.

## 6. İlk giriş ve kapanış

1. `https://ornek.com/yonetim` adresinden `ADMIN_EMAIL` / `ADMIN_PASSWORD` ile girin.
2. **Ayarlar** sayfasından parolanızı değiştirin.
3. `deploy/.env` içinden `ADMIN_PASSWORD` satırının değerini silin ve `docker compose -f deploy/docker-compose.yml up -d` çalıştırın.
   (Hesap veritabanında; bu satır bir daha okunmaz, dosyada durması sadece risk.)

## 7. Canlıya almadan önce: bunları deneyin

Bu listeyi atlamayın. Her madde gerçek bir "üye geldi ama çalışmadı" durumunu önler.

- [ ] **E-posta gidiyor mu?** Yönetim panelinde **Ayarlar → E-Posta Gönderimi → Test E-postası Gönder**'a basın.
      Kendi adresinize bir e-posta gelmeli. Gelmezse ekranda sunucunun verdiği neden yazar (ör. "535 …
      Username and Password not accepted" = kullanıcı adı/parola yanlış; "Could not connect to SMTP host" =
      sunucu adı ya da port yanlış). Düzeltip `deploy/.env`'i kaydedin, `docker compose -f deploy/docker-compose.yml up -d`
      çalıştırın ve tekrar deneyin. Sonra kendi başka bir adresinizle test üyesi açın: e-posta gitmezse üyenin
      sayfasında kırmızı bir uyarı çıkar ("Onay kodu e-postayla gönderilemedi"); düzelince **Kodu Tekrar Gönder**.
- [ ] **Üye girişi.** `https://ornek.com/uye` adresinden test üyesiyle girin; kod e-postayla gelmeli.
- [ ] **Antrenman ve beslenme.** Test üyesine plan atayın, üye olarak bir antrenman kaydedin.
- [ ] **Fotoğraf.** Üye olarak bir fotoğraf yükleyin, koç olarak görün.
- [ ] **Havale akışı.** Test siparişi verin, IBAN bilgileri doğru görünüyor mu, koç olarak ödemeyi onaylayınca üyelik başlıyor mu?
- [ ] **Kartla ödeme** (kullanıyorsanız). **Sandbox** anahtarlarıyla bir deneme ödemesi yapın ve siparişin ödenmiş görünmesini bekleyin.
      Ancak ondan sonra `PAYMENT_IYZICO_BASE_URL=https://api.iyzipay.com` ve canlı anahtarlara geçin.
      `PAYMENT_IYZICO_CALLBACK_URL` tam olarak `https://ornek.com/api/payments/iyzico/callback` olmalı.
- [ ] **Telefona kurulum.** Telefonda siteyi açıp "ana ekrana ekle" ile kurun; internet kapalıyken
      antrenman sayfasının açıldığını görün.
- [ ] **Yedek ve geri yükleme provası** (bir sonraki bölüm). Yedeğin geri yüklenebildiğini görmediğiniz sürece yedeğiniz yoktur.

Denemeler bitince test üyesini ve test siparişlerini silin.

## 8. Yedekleme

Yedeklenmesi gereken iki şey var: **veritabanı** ve **üyelerin fotoğrafları**. İkisini birden alan betik hazır:

```bash
deploy/backup.sh
```

`/var/backups/s2000` altına `db-…dump` ve `photos-…tar.gz` yazar, 14 günden eskileri siler
(`deploy/.env` içindeki `BACKUP_DIR` ve `BACKUP_KEEP_DAYS` ile değişir). Her gece 03:00'te çalışması için:

```bash
( crontab -l 2>/dev/null; echo '0 3 * * * /opt/s2000/deploy/backup.sh >> /var/log/s2000-backup.log 2>&1' ) | crontab -
```

**Aynı sunucudaki yedek yedek değildir.** Sunucu bozulursa, silinirse ya da hesabınız kapanırsa o da gider.
Yedek klasörünü düzenli olarak **başka bir yere** kopyalayın: örneğin başka bir sağlayıcıdaki depolama alanına
[`restic`](https://restic.net) ile (şifreli, tekrar eden yedeği kendisi yönetir) ya da `rclone`/`rsync` ile.
Dosyalarda sağlık verisi olduğu için **şifreli** saklayın ve erişimi kısıtlayın.

Kopyayı kendiliğinden almak için `deploy/.env` içine `BACKUP_COPY_CMD` yazın; `backup.sh` her yedekten sonra onu
çalıştırır (`BACKUP_DIR` ve `STAMP` değişkenleri kullanılabilir). Kopya başarısız olursa betik de başarısız olur,
yani cron size e-posta atar. Örnek (şifreli bir `rclone` "crypt" uzak hedefi kurduktan sonra):

```
BACKUP_COPY_CMD=rclone copy "$BACKUP_DIR" s2000crypt: --include "*-$STAMP.*"
```

### Yedeğin gerçekten geri yüklenebildiğini kendiliğinden kontrol etme

```bash
deploy/verify-backup.sh
```

En yeni yedeği **canlı veritabanına dokunmadan**, geçici bir PostgreSQL kapsayıcısına yükler ve şunlara bakar:
dump okunabiliyor mu, içinde koç hesabı ve şema geçmişi var mı, veritabanının işaret ettiği **her fotoğraf dosyası
arşivde mi**, ve yedek 36 saatten (`BACKUP_MAX_AGE_HOURS`) eski değil mi (eskiyse gece yedeği durmuş demektir).
Sorun varsa nedenini yazar ve sıfırdan farklı kodla çıkar. Haftada bir çalıştırın:

```bash
( crontab -l 2>/dev/null; echo '30 4 * * 1 /opt/s2000/deploy/verify-backup.sh >> /var/log/s2000-backup.log 2>&1' ) | crontab -
```

Betiği bir dump ile (sağlam, bozuk, fotoğrafı eksik arşiv, bayat yedek) denedim; her durumda doğru sonuç verdi.
Cron'un kendisi ve `rclone` ile sunucu dışına kopyalama bu ortamda denenmedi.

### Geri yükleme

```bash
deploy/restore.sh /var/backups/s2000/db-20261028-030000.dump /var/backups/s2000/photos-20261028-030000.tar.gz
```

Betik sizden `EVET` yazmanızı ister, çünkü **mevcut veritabanını ve fotoğrafları yedektekilerle değiştirir**.
Backend'i durdurur, geri yükler, tekrar başlatır. Yedeği aldıktan sonra yapılan her şey kaybolur.

**Prova (yılda en az bir kez, ve ilk kurulumda):** bir üye ekleyin ve yedek alın, üyeyi silin, geri yükleyin,
üyenin geri geldiğini görün. Rehberi yazarken bunu yığın üzerinde denedim: üye ve fotoğraf dosyası geri geldi,
koç girişi çalıştı.

## 9. Güncelleme

Yeni bir sürümü yayına alırken:

```bash
cd /opt/s2000
deploy/backup.sh                                   # önce yedek
git pull
docker compose -f deploy/docker-compose.yml up -d --build
docker compose -f deploy/docker-compose.yml logs -f backend
```

Veritabanı değişiklikleri backend açılırken kendiliğinden uygulanır. Bir şey ters giderse: `git checkout <önceki sürüm>`,
`up -d --build`; veritabanı şeması ilerlediyse yukarıdaki yedeği geri yükleyin. Büyük güncellemelerden önce yedek almayı atlamayın.

## 10. Her gün bakılacak şeyler

- **Erişilebilirlik izleme.** Ücretsiz bir servis (ör. UptimeRobot) `https://ornek.com` adresine bakıp site düşünce size e-posta atsın.
- **Disk.** `df -h` ile ara sıra bakın; fotoğraflar ve yedekler büyür. Dolarsa veritabanı yazamaz.
- **Günlükler.** `docker compose -f deploy/docker-compose.yml logs --tail 200 backend`. Günlük dosyaları otomatik döner (5 × 10 MB).
- **Sertifika.** Caddy kendisi yeniler; yeter ki **80 ve 443 portları açık kalsın** (80 de gerekli, doğrulama için).
- **Sunucu güncellemeleri.** Güvenlik güncellemeleri otomatik; çekirdek güncellemesinden sonra ara sıra `reboot` gerekir.
  Yeniden başlatma sonrası konteynerler kendileri açılır (`restart: unless-stopped`).

## Güvenlik özeti

Hazır gelenler: yalnızca 80/443 açık; veritabanı ve backend dışarıdan görünmez; backend root olmayan kullanıcıyla
çalışır; HTTPS zorunlu (HSTS); sayfalar yalnızca kendi dosyalarını, Google Fonts'u, ana sayfadaki stok
fotoğrafları ve YouTube'un çerezsiz oynatıcısını yükleyebilir (CSP); başka sitelerin sayfayı çerçeveye almasına
izin verilmez; `/api` istek boyutu 12 MB ile sınırlıdır; 404 yanıtları önbelleğe alınmaz.

**Sizin sorumluluğunuzda kalanlar:** güçlü ve benzersiz parolalar, SSH anahtarı, sunucu güncellemeleri,
yedeklerin sunucu dışına şifreli kopyası, `deploy/.env` dosyasının gizli kalması, KVKK yükümlülükleri
(aydınlatma, saklama süresi, veri sahibi başvuruları).

## Sorun giderme

| Belirti | Olası neden ve çözüm |
| --- | --- |
| Site açılmıyor / sertifika hatası | DNS A kaydı sunucuyu göstermiyor ya da 80/443 kapalı. `docker compose -f deploy/docker-compose.yml logs web` içinde "challenge" ya da "dns" hatası aranır. Çok sık denemek sertifika sağlayıcısının limitine takılır; önce DNS'i düzeltin. |
| `up` komutu "… is not set in deploy/.env" diyor | Zorunlu bir ayar boş: `DB_PASSWORD`, `PUBLIC_URL`, `ADMIN_EMAIL`, `MAIL_HOST`, `MAIL_FROM`, `SITE_ADDRESS`. |
| Backend "unhealthy" ya da sürekli yeniden başlıyor | `logs backend`. Sık neden: `DB_PASSWORD` sonradan değiştirilmiş (yukarıdaki nota bakın) ya da bellek yetersiz. |
| Üye kodu gelmiyor | SMTP ayarı. Önce Ayarlar'daki test e-postasına bakın (nedeni ekranda yazar); ayrıntı için `logs backend` içinde "gönderilemedi" arayın. Port 465 kullanıyorsanız `MAIL_SSL=true` ve `MAIL_STARTTLS=false`. Spam klasörüne de bakın. |
| Fotoğraflar yok | `photos` volume'u silinmiş ya da yeni sunucuya taşınırken yedekten alınmamış. `docker volume ls` içinde `s2000_photos` olmalı. |
| 80/443 "already in use" | Sunucuda başka bir web sunucusu çalışıyor (`apache2`, `nginx`). Durdurun ya da kaldırın. |
| Telefondaki eski sürüm | Site yeniden derlenince telefon yeni sürümü bir sonraki açılışta alır; kapatıp açmak yeter. |

## Bu rehberde neler denendi, neler denenmedi

Dürüst olmak gerekirse, rehberin hazırlanışında bir geliştirme ortamında şunlar **gerçekten çalıştırıldı**:
yığının ayağa kalkması (sağlık kontrolleri dahil), Caddy üzerinden yönetim girişi, güvenlik başlıkları ve
tarayıcıda içerik politikası (CSP ihlali çıkmadı), servis çalışanının (offline) kaydı, derin bağlantılar,
`/assets` ve hata yanıtlarının önbellek başlıkları, 12 MB sınırı (13 MB'lık yükleme reddedildi), backend'in
root olmaması, ve **yedek alma → veriyi bozma → geri yükleme** (üye ve fotoğraf dosyası geri geldi, yedek
betiğindeki bir hata bu denemede bulunup düzeltildi).

**Denenmedi:**
- Dockerfile'lardaki derleme adımları konteyner *içinde* (geliştirme ortamının ağ kısıtı yüzünden): aynı komutlar
  (`gradlew bootJar`, `npm ci`, `npm run build`) sunucuda çalıştırıldı, imajların çalışma aşamaları ve yığın o
  çıktılarla denendi. İlk gerçek `up --build` sizin sunucunuzda olacak; bir derleme hatası çıkarsa bana iletin.
- Gerçek bir alan adıyla otomatik HTTPS sertifikası alınması (burada yerel, kendinden imzalı sertifika kullanıldı).
- Gerçek bir SMTP sağlayıcısıyla e-posta gönderimi ve iyzico (sandbox da dahil). E-posta uyarıları yerel bir sahte SMTP
  sunucusuyla denendi (parolayı reddeden, her şeyi kabul eden ve hiç tanımlı olmayan üç durum).
- Bu rehberdeki Ubuntu komutları (ufw, unattended-upgrades, cron, Docker kurulumu) ve sunucu dışına yedek kopyalama.
- Windows'ta `baslat.bat` / `yedekle.bat` — bunlar yalnızca geliştirme içindir, sunucuda kullanılmaz.
