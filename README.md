# S2000 Coaching

Online fitness & beslenme koçluğu tanıtım sitesi, sipariş akışı, üye alanı ve yönetim paneli. `stitch_fitcoach_premium_platform/` ve
`stitch_fitcoach_premium_platform (1)/` altındaki iki Stitch tasarımının birleştirilmiş halidir (bkz. bu
klasörlerdeki `DESIGN.md`).

## Yapı

- `backend/` — Spring Boot 4 (Java 21, Gradle, PostgreSQL, Liquibase). Paket, sipariş ve üyelik REST API'si.
- `frontend/` — React 19 + Vite + TypeScript + Tailwind v4 + React Router. Landing sitesi, üye alanı ve
  yönetim paneli.
- `docker-compose.yml` — yerel geliştirme için yalnızca PostgreSQL (host portu **7002**; frontend 7000, backend 7001 — yaygın 3000/8080/5432 ile çakışmasın diye).

### Sayfalar

| Yol | Sayfa | Erişim |
| --- | --- | --- |
| `/` | Tanıtım sitesi, paketler ve checkout | Herkese açık |
| `/hesaplayicilar` | Kalori (Mifflin–St Jeor), makro ve VKİ hesaplayıcıları | Herkese açık; girilenler tarayıcıdan çıkmaz, kaydedilmez |
| `/giris` | Tek giriş sayfası (üyeler ve koç) | Herkese açık |
| `/uye` | Üye alanı | Giriş yapmış üyeler |
| `/yonetim` | Yönetim paneli | E-posta + şifre ile giriş yapan koç hesapları |

**Üye alanı** (masaüstünde sol menü, telefonda alt sekme çubuğu): `/uye` bugün · `/uye/antrenman` (ve
`/uye/antrenman/:gunId` gün detayı) · `/uye/beslenme` · `/uye/verilerim` · `/uye/olcumler` · `/uye/gecmis` ·
`/uye/fotograflar` · `/uye/mesajlar` · `/uye/profil`.

`/uye/gecmis` ve `/uye/fotograflar` telefon sekme çubuğunda yoktur: çubuk genişliği sekmelere böler, altıdan sonrası
parmak boyutunun altına iner. Sayfalara bugün ekranındaki kısayol kartlarından girilir
(`PortalNavItem.desktopOnly`).

**Bugün ekranı** üç şey gösterir: 10 gündür ölçüm girilmediyse (ya da hiç girilmediyse) ölçüm
hatırlatıcısı, sıradaki antrenman, ve beslenme planından gelen günün makroları. Makro kartı hem
plandaki hedefi hem öğünlerin toplamını gösterir; ikisi birlikte varsa çubuk planın hedefe ne kadar
yaklaştığını söyler — planın kendi içinde tutarlı olup olmadığını görmenin yolu budur.

> Hatırlatıcının eşiği (`MeasurementReminder.STALE_AFTER_DAYS`) koçun panelindeki "ölçüm girmeyenler"
> listesiyle (`AdminDashboardService.STALE_AFTER_DAYS`) aynı olmak zorundadır; farklı olurlarsa üyeye
> "güncelsin" derken koça aynı üyeyi kovalaması söylenir.

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

> **Windows'ta tek tık:** `baslat.bat` veritabanını, backend'i ve ön yüzü **tek pencerede** başlatır; ikisinin
> çıktısı `loglar\backend.log` ve `loglar\frontend.log` dosyalarına yazılır, bir şey ters giderse son
> satırlar pencerede gösterilir. Pencerede bir tuşa basmak (ya da pencereyi kapatmak) ikisini durdurur.
> `durdur.bat` hepsini kapatır (veritabanı dahil), `sifirla.bat` onay sorarak geliştirme veritabanını siler.
> Bu üç dosya Windows'ta denenmedi.


**Windows'ta tek komut:** proje kökündeki `baslat.bat` dosyasına çift tıklayın (ya da CMD'de
`baslat.bat` yazın). Betik gerekli programları kontrol eder, PostgreSQL'i açar, hazır olmasını
bekler, backend ile frontend'i kendi pencerelerinde başlatır ve hazır olunca tarayıcıyı açar.
İlk koç hesabının e-postası ve şifresi betiğin başındaki üç satırda tanımlıdır; değiştirmek
isterseniz orayı düzenleyin.

Elle çalıştırmak isterseniz (ya da Linux/macOS'taysanız):

```bash
# 1) Veritabanı
docker compose up -d postgres

# 2) Backend (varsayılan port 7001; meşgulse SERVER_PORT ile değiştirin)
#    ADMIN_EMAIL ve ADMIN_PASSWORD tanımlı değilse koç hesabı oluşmaz ve
#    yönetim paneline giriş yapılamaz.
cd backend
ADMIN_EMAIL=koc@ornek.com ADMIN_PASSWORD=GucluBirSifre ./gradlew bootRun

# 3) Frontend
cd frontend
cp .env.example .env   # VITE_API_BASE_URL'i backend portunuza göre düzenleyin
npm install
npm run dev
```

Windows CMD'de `./` öneki yoktur (`gradlew bootRun`), ortam değişkenleri de ayrı satırda
`set ADMIN_EMAIL=...` ile tanımlanır.

Frontend `http://localhost:7000`, backend varsayılan olarak `http://localhost:7001` üzerinde
çalışır. Yönetim paneline `ADMIN_EMAIL` / `ADMIN_PASSWORD` ile girilir.

**Gerekenler:** Java 21, Node 22, Docker (yalnızca PostgreSQL için).

## Yayına alma

Siteyi gerçek üyeler için bir sunucuda çalıştırmak: **[docs/yayin-rehberi.md](docs/yayin-rehberi.md)**
(sunucu, alan adı, HTTPS, e-posta, ödeme, yedekleme ve güncelleme adım adım). Üretim yığını
`deploy/` klasöründedir: `docker-compose.yml`, `Caddyfile` (HTTPS + güvenlik başlıkları), `.env.example`,
`backup.sh` ve `restore.sh`. Saat dilimi tek bir ayardan gelir: `APP_TIME_ZONE` (varsayılan
`Europe/Istanbul`; ön yüz için `VITE_APP_TIME_ZONE`).

## Üyelik akışı

**Hesap ve giriş (güncel).** Sitenin üst menüsünde tek bir **Giriş Yap** düğmesi vardır (`/giris`): iki sekme,
**Giriş Yap · Üye Ol**. Yönetim için ayrı bir sekme ya da sayfa yoktur: üyeler de koç da **aynı formdan**
e-posta + şifreyle girer; sunucu (`POST /api/auth/login`) adresin koça mı üyeye mi ait olduğuna bakar, koç
`/yonetim`'e, üye `/uye`'ye yönlenir. Hatalı şifre ile bilinmeyen adres aynı mesajı verir. Oturum açmadan
`/yonetim` ya da `/uye` açılırsa `/giris`'e yönlenir. Giriş yapılmışsa düğme "Hesabım" / "Yönetim Paneli" olur. Üye **Üye Ol** sekmesinden
hesabını açar (ad, soyad, e-posta, yaş, cinsiyet, boy, kilo, spor geçmişi, şifre ve aydınlatma onayı).
E-posta doğrulaması açıkken (varsayılan, canlıda böyledir) hesap, adrese gelen 6 haneli kod girilene kadar
giremez; yerel `baslat.bat` bunu kapatır (`REGISTRATION_VERIFY_EMAIL=false`), çünkü yerelde e-posta sunucusu
yoktur ve kayıt olan hemen girer. Beş yanlış şifre hesabı 15 dakika kilitler; yanlış adres ile yanlış şifre aynı
mesajı verir. Seçilen her şifre (kayıt, sıfırlama, değiştirme, koçun belirlediği) **en az 8 karakter** (koç hesabı için 10),
en çok 64 (bcrypt 72 baytı okur) olmalı ve **bir büyük harf, bir küçük harf, bir rakam ve bir özel karakter**
içermelidir; kurallar yazarken tek tek işaretlenir, sunucu aynı kuralı `@StrongPassword` ile zorlar. Giriş
isteği bu kurala bağlı değildir (eski şifreli hesaplar girebilsin diye). Şifre alanlarında göz simgesi şifreyi
gösterir/gizler. Yalnızca bcrypt özeti saklanır.

**Hesap çakışması ve üye numarası.** Bir e-posta adresi tek hesaba aittir: üyeler arasında olduğu gibi **üye ile
koç hesabı arasında da** (ikisi aynı formdan girdiği için); kayıt, koçun üye açması, üyenin e-postasını
değiştirmesi ve `ADMIN_EMAIL` tohumlaması bunu denetler, adres büyük/küçük harf ve baştaki/sondaki boşluktan
bağımsız karşılaştırılır ve iki isteğin yarışması veritabanındaki tekillik kısıtıyla 409'a çevrilir. Her üyeye
kayıt anında **rastgele, benzersiz 5 haneli bir üye numarası** verilir (`#12345`; sıralı değildir, kimin önce
katıldığını göstermez), sonradan değiştirilemez (sütun güncellenemez, hiçbir güncelleme isteği taşımaz). Üye
yalnızca **Profil** sayfasındaki Üyelik kartında, koç üyenin Profil sekmesinde görür; aynı adı taşıyan üyeleri
ayırt etmek içindir. Var olan üyelere geçişte rastgele numara atanır.
**Şifremi unuttum** (ve koçun açtığı, henüz şifresi olmayan üyeler) e-posta koduyla yeni şifre seçer; şifre
değişince o üyenin diğer oturumları kapanır. Üye yaş, telefon, boy, kilo ve spor geçmişini düzenler; **ad soyad,
cinsiyet ve e-postayı yalnızca koç değiştirir**. Koç, üye sayfasında her bilgiyi düzenler ve üyeye şifre belirler
(şifre geri okunamaz, yalnızca değiştirilir).

**Koç hesabı.** `ADMIN_EMAIL` / `ADMIN_PASSWORD` ile tanımlanan hesap, veritabanında yoksa başlarken oluşturulur
(başka bir koç hesabı olsa bile; böylece yeni bir koç eklemek için yeni adresle bir kez başlatmak yeter). Var olan
hesaba dokunulmaz: şifreyi sonradan Ayarlar'dan değiştirdiysen buradaki değer etkisizdir. Yerelde bu bilgileri
`yerel-ayarlar.bat` dosyasına yaz (örneği `yerel-ayarlar.ornek.bat`); dosya git'e girmez. Gerçek şifreyi depoya,
README'ye ya da sohbete yazma.

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

**Dinamik plan düzenleyici.** Plan **Gün 1** ile açılır ve seçilidir; diğer günler günlerin yanındaki **+**
ile tek tek eklenir (**en az 1, en fazla 7 gün**; son gün silinemez). Seçili günde egzersizlerin listesi ve
altında yeni egzersiz formu durur. **Egzersiz adını koç yazar** (kataloğa bağlı bir seçim kutusu yoktur):
aynı ad (büyük/küçük harf fark etmez) kataloğdaki kaydı yeniden kullanır, yoksa o anda kataloğa eklenir —
video bağlantısı ve geçmiş de böylece adla birlikte kalır; kas grubu bu yoldan eklenenlerde boştur
(Hareketler sayfasından doldurulabilir). Yeni egzersizde **1.set** hazır gelir (tekrar kutusu boş);
**Set ekle** (+) ile **en fazla 10 ssete** kadar yenisi eklenir, istenmezse yalnızca 1.set ile
**Egzersizi Kaydet**'e basılır. Sondaki boş setler yok sayılır; ortada boş bırakılıp sonrası doldurulan set
reddedilir (setleri sessizce yeniden numaralamak olurdu). Kaydedilmiş egzersizler aynı biçimde **Güncelle** ile
değişir; hedef kg, dinlenme saniyesi ve üyenin göreceği not isteğe bağlıdır. Eski biçimde (set sayısı +
serbest metin) yazılmış egzersizler olduğu gibi çalışır.

**Egzersiz görseli.** Her egzersizin bir **demonstrasyon görseli** olabilir (ekran görüntüsü): koç, plan
düzenleyicisinde kaydedilmiş egzersizin altından ya da **Hareketler** sayfasından **Görsel Yükle** der
(PNG ya da JPEG, en fazla 8 MB). Görsel kataloğdaki harekete bağlıdır; o hareketi kullanan her plan ve her
üye aynı görseli görür, değiştirmek ya da kaldırmak hepsine yansır. Yüklenen dosya çözülüp yeniden
kodlanır (konum/cihaz gibi üst veri atılır, resim olmayan dosya reddedilir), yalnızca depolama anahtarı
veritabanında tutulur; hareket silinirse dosyası da silinir. Üye görseli gün sayfasında egzersizin
başında görür; görsele yalnızca giriş yapmış üye ya da koç erişir.

**Üye yalnızca görüntüler.** Üye `/uye/antrenman` gün sayfasında koçun yazdığı setleri (1.set, 2.set …),
notu ve videoyu salt okunur görür; plan üzerinde hiçbir düzenleme ucu üyeye açık değildir. Gerçekte
kaldırdığını kaydetmek isterse **Bugünkü Antrenmanımı Kaydet** ile kayıt kutuları açılır (ağırlık takibi,
RPE ve öneriler bu kayıttan beslenir).

**Geçmiş antrenman girme.** Kayıt kutuları "Bu antrenmanı hangi gün yaptınız?" tarih seçicisiyle açılır
(Bugün/Dün kısayolları; gelecek tarih seçilemez). `/uye/gecmis` sayfasındaki **Geçmiş Antrenman Ekle**
bölümünden planın günü ve tarih seçilip ilgili gün sayfası o tarih için açık gelir. O güne ve tarihe zaten
kayıt varsa değerler onunla dolu gelir, uyarı gösterilir ve kaydetmek yeni satır açmaz, o seansı günceller;
geçmiş listesindeki her seansta **Düzenle** bağlantısı da aynı yere götürür (yalnızca aktif planın günleri
için). Tarih değiştirilince yazılmış değerler yeni tarihin kaydıyla değiştirilir.
Yanlış girilen ya da hiç yapılmamış bir seans geçmiş listesindeki çöp kutusuyla (onay sorarak) silinir;
silme tarih sınırına bağlı değildir.

**Antrenman 1 – 10.** Üyenin gün sayfasında her hareketin altında on kutu vardır; kutular üyenin zaten
kaydettiği seanslardan dolar (o seansın en ağır seti), yani ağırlık tek yerden girilir. Onuncudan sonra
pencere kayar ve numaralar sürer (Antrenman 3 – 12). Üye bir değer yazmaya başlayınca "bugün" kutusu
yazdığını gösterir; dokunmadan önce, geçen seansın ön-dolu değerlerini bugün kaldırılmış ağırlık gibi
göstermemek için boştur.

Koç, üye detayındaki Antrenman sekmesinin altında üyenin son seanslarını görür.

## Beslenme planları

Antrenmanla aynı kalıp: `/yonetim/sablonlar` sayfasının **Beslenme** sekmesinde şablon yazılır (öğünler ve
her öğünde besin + miktar + kalori/makro), üye detayındaki **Beslenme** sekmesinden üyeye kopyalanır. Kopya
yine bağımsızdır ve üyenin yalnızca bir planı aktiftir.

**Antrenman günleri ve dinlenme günleri.** Plan iki yarıdan oluşur: **Antrenman Günleri** ve **Dinlenme
Günleri**. Her yarının kendi kalori/protein/karbonhidrat/yağ hedefi ve kendi öğünleri/supplement blokları
vardır (su ve plan notu tüm plan için ortaktır); düzenleyicide ve üyenin sayfasında iki sekmeyle
değiştirilir, "Bugün" sayfasındaki makro kartı da aynı seçimi sunar (plan bugünün hangi tür olduğunu
bilmez, üye seçer). Bir öğün hangi yarıya eklendiyse orada kalır. Önceden yazılmış planlarda mevcut her
öğün ve hedef **antrenman günü** sayılır; dinlenme günü tarafı boş başlar, koç doldurur. Yazdırılan çıktıda
dinlenme günü yarısı, doluysa, ayrı sayfada çıkar.

**Standart şema.** Şablon seçmeden plan oluşturduğunuzda (Planı Oluştur) plan koçun kendi planlarındaki
şemayla açılır (antrenman günü yarısı tam, dinlenme günü yarısı antrenman öncesi/sonrası öğünleri ve
antrenman sonrası supplementi olmadan): Sabah, Öğlen, Antrenman öncesi/sonrası, Akşam, Uyumadan önce öğünleri ve üç supplement
bloğu, besinler yazılı, **miktarlar boş**. Miktar, kalori ve su hedefi üyeye özeldir, bu yüzden şemada
hiç yoktur; boş bir kutu "reçete edilmedi" demektir, "sıfır" değil. Plan her öğün/supplement için bir
tablodur: besin solda, miktar sağda; miktar kutusuna yazıp Enter'a basınca bir alttakine geçer, tablonun
altındaki **+** besin ekler, çöp kutusu çıkarır; öğün ve supplement bloğu da aynı şekilde eklenip silinir.
`Tamamen boş plan` seçeneği şemayı atlar. Üye aynı tabloyu salt okunur görür; girilmemiş miktar "—"
olarak çıkar.

Plan düzeyinde **makro hedefi** (kalori/protein/karbonhidrat/yağ) tutulur; arayüz bunun yanında öğünlerin
fiili toplamını da gösterir — aradaki fark planın henüz tamamlanmadığını söyleyen şeydir. Makro girmek
zorunlu değil: besinleri makro yazmadan da reçete edebilirsiniz.

Üye `/uye/beslenme` sayfasında seçtiği gün türünün hedeflerini ve öğünlerini salt okunur görür.

## Plan çıktısı (yazdır / PDF)

Koçun elle hazırladığı plan kâğıdının yerini tutar. Koç: üye sayfasındaki **Plan Çıktısı** düğmesi
(`/yonetim/uyeler/:id/cikti`); üye: Antrenman ve Beslenme sayfalarındaki **Yazdır / PDF** (`/uye/cikti`).
Sayfa üyenin **aktif** planlarından yatay A4 bir belge çizer: üye bilgileri ve günlük hedefler, öğün ve
supplement tabloları (miktarı yazılmamış kalem elle doldurulacak boş bırakılır), her gün için set 1–4
tablosu ve her hareket için **boş "Antrenman 1–10" ağırlık kutuları**. **Yazdır / PDF** düğmesi tarayıcının
yazdırma penceresini açar; yazıcı olarak "PDF olarak kaydet"i seçince dosya olur. Menü, başlık ve düğmeler
çıktıya girmez.

Belge yalnızca sistemde olanı basar: ölçüm yoksa üye kartında yalnızca boy ve başlangıç kilosu vardır; her
ölçünün **en son** değeri alınır; VKİ boy ve güncel kilodan hesaplanan düz formüldür (yağ oranı, FFMI ya
da bakım kalorisi gibi veri girilmeyen değerler uydurulmaz). Hareketsiz günler basılmaz.

## Mesajlaşma

Üye başına tek bir yazışma dizisi, iki taraflı: üye `/uye/mesajlar`, koç üye detayındaki **Mesajlar**
sekmesi. Diziyi açmak **karşı tarafın** mesajlarını okundu işaretler — okunmamış rozetlerini ve koçun
cevap kuyruğunu sürükleyen şey budur. Gönderenin kendi balonunda mesaj henüz okunmadıysa "okunmadı"
yazar.

Yönetim paneli, **son mesajı okunmamış** (yani cevap bekleyen) üyeleri ayrı bir blokta listeler; koç
diziyi açtığı anda üye bu kuyruktan düşer.

### Mesaj merkezi (`/yonetim/mesajlar`)

Koç tüm yazışmaları tek listede görür: cevap bekleyenler en üstte, sonra en son hareket eden dizi.
Liste her üyeyi içerir, böylece hiç yazışmamış bir üyeyle de dizi başlatılabilir. Listeyi ve rozetleri
besleyen uçlar (`/api/admin/messages/threads`, `/api/admin/messages/unread`,
`/api/member/messages/unread`) hiçbir mesajı okundu işaretlemez — aksi hâlde düzenli yoklanan rozet
kendi kendini temizlerdi.

### Hazır yanıtlar

Koçun sık yazdığı mesajlar `message_template` tablosunda durur ve yazma alanının üstünde çip olarak
çıkar. Çip metni taslağın **sonuna eklenir**, üzerine yazmaz; koç yazdığı bir mesajı "Yazdığını kaydet"
ile yeni bir hazır yanıta çevirebilir. Başlıklar benzersizdir.

### Kolay iletişim

- Üye ve yönetim menülerindeki **Mesajlar** girdisi okunmamış sayısını rozet olarak gösterir (60 sn'de
  bir yoklanır); açık bir dizi 15 sn'de bir yenilenir.
- Üyenin **Bugün** ekranındaki *Koçuna Sor* kartı, sık sorulan soruları tek dokunuşla hazır mesaj
  olarak açar (`/uye/mesajlar?soru=…`).
- Yazma alanı **Ctrl + Enter** ile gönderir; Enter satır atlar (üyeler telefondan yazıyor). Dizide gün
  ayraçları (Bugün / Dün / tarih) ve gönderenin kendi balonunda okundu/okunmadı bilgisi vardır.

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

Ölçümler ve gelişim fotoğrafları KVKK'da **özel nitelikli kişisel veri** sayılır, bu yüzden
checkout'taki KVKK onayı bunları kapsamaz. Üye, ölçüm girmeden ya da fotoğraf yüklemeden önce aynı
onayı vermek zorundadır; onay verilene kadar sunucu hiçbir satır yazmaz
(`member.health_data_consent_at`). Onay verilmemesi üyeliği etkilemez, yalnızca takip kapalı kalır.

Üye silindiğinde ölçüm satırları, fotoğraf satırları **ve fotoğraf dosyaları** birlikte silinir.
Yalnızca satırları silmek, silinme talebinde bulunmuş birinin vücut fotoğraflarını diskte bırakırdı.

> **Yapılacak:** `frontend/src/components/member/HealthDataConsent.tsx` içindeki aydınlatma metni
> **taslaktır**. Yayına çıkmadan önce kendi hukuk danışmanınızın onayladığı metinle değiştirin.

## Gelişim fotoğrafları

Üye `/uye/fotograflar` sayfasından fotoğraf yükler, siler ve **önce/sonra karşılaştırması** yapar.
Koç aynı fotoğrafları üye detayındaki **Fotoğraflar** sekmesinden görür ve silebilir, ama **yükleyemez**
— birinin vücut fotoğrafını oraya koymak yalnızca kendi kararıdır.

Karşılaştırma **yalnızca aynı duruştaki** (ön / yan / arka) iki fotoğrafı yan yana koyar; önden çekilmiş
bir fotoğrafın yanına arkadan çekilmiş birini koymak gelişim hakkında bir şey söylemez. Varsayılan
olarak o duruşun en eski ve en yeni fotoğrafı seçilir, yani eldeki en geniş aralık.

Yüklemede kabul edilenler: **JPEG, PNG, WebP**. SVG ve HTML bilerek reddedilir — kendi alan adımızdan
geri servis edilen böyle bir dosya script çalıştırabilirdi.

### Depolama

Veritabanında yalnızca **anahtar** tutulur, baytlar değil: görüntüler her yedeği ve bu tabloya dokunan
her sorgu planını şişirirdi. Baytlar `PhotoStorage` arayüzünün arkasındadır:

- `FilesystemPhotoStorage` bugünkü tek uygulamadır ve `PHOTO_STORAGE_PATH` altına yazar.
- Anahtarlar burada üretilir ve çağıranlar için anlamsızdır: iki seviyeli dağıtım altında rastgele bir
  ad. Yüklemeden hiçbir şey — ne üye kimliği ne özgün dosya adı — anahtara girmez, böylece sızan bir
  anahtar bir şey söylemez ve kötü niyetli bir dosya adı kök dizinden çıkamaz.
- **Tek sunucu** çözümüdür. Birden fazla makinede çalışan bir kurulum ya paylaşılan bir birim ya da
  `PhotoStorage` arayüzünün nesne depolama (S3 vb.) uygulamasını ister — şema değişmeden eklenebilir.

Satır yazılmadan önce baytlar yazılır: hiçbir şeye işaret etmeyen bir satır arayüzde kırık bir fotoğraf
demektir, satırı olmayan baytlar ise görünmez ve geri kazanılabilir.

## Koçluk özellikleri

Hepsi `/uye` (üye) ve `/yonetim/uyeler/:id` (koç) altında; sağlık verisi sayılanlar KVKK onayı ister.

- **İlgi bekleyenler** (`/yonetim`): tüm aktif üyeleri tek geçişte (üye başına sorgu yok) şu nedenlerle
  puanlar — bekleyen mesaj, biten/biten üyelik, antrenman/ölçüm/haftalık beslenme kaydı yok, yanıt bekleyen
  değerlendirme ve form videosu, sağlık anketinde dikkat gerektiren yanıt. En acilden sıralı, her
  neden yazılı. Sessizlik, üyenin katıldığı günden sayılır; plan verilmemiş konuda kimse "kayıt yok"
  diye işaretlenmez. Havale bekleyen siparişler en üstte.
- **Haftalık beslenme albümü** (`/uye/beslenme`): üye bir **hafta** (pazartesi–pazar, örn. 26.10.2026 –
  01.11.2026) seçer; o haftanın **toplam kalorisini** ve **karbonhidrat / yağ / protein yüzdelerini** girer
  (toplam tam 100 olmalı, değilse kaydedilmez) ve haftaya **tek bir fotoğraf** ekleyebilir. Gramajlar
  otomatik hesaplanır (karbonhidrat ve protein 4, yağ 9 kcal/g) ve **haftalık toplam ile günlük ortalama**
  (hafta ÷ 7) kartta gösterilir; gram değerleri saklanmaz, kalori ve yüzdelerden her seferinde türetilir.
  Aynı hafta tekrar kaydedilirse güncellenir (kalemle düzenle, çöp kutusuyla sil); **geçmiş haftalar** da girilir: hafta listesi son bir yılı verir, **tarih seçiciyle**
  seçilen günün haftası (pazartesi–pazar) üç yıl geriye kadar açılır ve "Girilmemiş son haftalar" düğmeleri
  eksik kalan son haftaları tek dokunuşla açar; gelecek haftalar ve üç
  yıldan eski haftalar girilemez. Koç aynı albümü üye detayı → Beslenme sekmesinde okur (fotoğraflar dahil).
  Sağlık verisidir: onay olmadan yazılmaz, üye silinince satırlar ve fotoğraf dosyaları da silinir. Eski
  günlük yemek kaydı ekranı kaldırıldı; daha önce girilmiş günlük kayıtlar koçun sekmesinde durur.
  "İlgi bekleyenler" artık haftalık kayda bakar: son girilen haftanın bittiği günden 10 gün geçtiyse işaretler.
- **Antrenman gelişimi** (`/uye/gecmis`, üye detayı → Antrenman): set başına **RPE**, hareket başına
  tahmini 1TM (Epley, yalnızca ≤12 tekrarlık ağırlıklı setlerden), "yeni rekor" (öncesinde yenilecek bir
  seans gerekir), 12 haftalık hacim. Gün ekranında her harekete **sonraki seans önerisi**: tüm çalışma
  setleri üst tekrar hedefinde ve RPE ≤ 8,5 ise ağırlık artışı; zor geçtiyse ya da aralığın altında
  kaldıysa aynı ağırlık. Veri yetmiyorsa (sayısal olmayan hedef, ağırlık yok) öneri verilmez; tek seansa
  dayanan öneri bunu söyler. Dinlenme sayacı saate göre sayar, arka planda kaymaz.
- **Haftalık değerlendirme** (`/uye/haftalik`): uyku/stres/enerji/açlık (1–5), beslenmeye uyum ve not;
  haftanın pazartesisiyle anahtarlanır, koç aynı karttan yanıtlar. Aynı haftayı yeniden göndermek eski
  yanıtı geçersiz kılar.
- **Üye analizi** (üye detayı → Analiz, yalnızca koç): vaka çalışmalarındaki motor, üyenin kendi ölçüm
  ve yemek kayıtlarında çalışır; veri yetmeyen yerde sayı yerine neden yazar. Üye satırları motora
  kaydedilmeden, bellekte verilir — vaka tablolarına üye verisi kopyalanmaz.
- **Başlangıç anketi** (`/uye/anket`): standart 7 PAR-Q sorusu + hedef, haftada kaç gün, ortam, sakatlık.
  "Evet" kimseyi engellemez; koça doktor onayı uyarısı olarak görünür. Plan oluştururken üyenin istediği
  gün sayısına uyan şablonlar ★ ile işaretlenir.
- **Form kontrolü** (`/uye/form-kontrol`): üye hareket videosunu YouTube'a **listelenmemiş** yükleyip
  bağlantıyı gönderir; koç sayfa içinde izler ve yazılı geri bildirim verir. Uygulama video barındırmaz,
  yalnızca bağlantıyı tutar (YouTube dışı adresler reddedilir).
- **Egzersiz videoları**: hareket kataloğunda ya da plan editöründe hareket başına YouTube bağlantısı;
  üye gün ekranında "Nasıl yapılır?" ile sayfa içinde izler. Oynatıcı adresi yalnızca video kimliğinden
  kurulur (`youtube-nocookie.com`), düğmeye basılana kadar yüklenmez.
- **Başarı hikâyeleri** (`/yonetim/hikayeler`, `/uye/hikayem`): koç, üyenin kendi gelişim fotoğraflarından
  hikâye yazar; fotoğraflar **kopyalanır** (yeniden kodlanır, GPS/cihaz bilgisi silinir) ve herkese açık
  uçlar hiçbir üye tablosuna dokunmaz. Hikâye ancak **üyenin kendi hesabından, o içeriğe açık rıza
  vermesiyle** yayınlanabilir; rıza metni kayda geçer, metin ya da fotoğraf değişirse rıza düşer, üye
  istediği an geri çekebilir (yayından kalkar, kopyalar silinir). Hiç yayınlanmış hikâye yokken bölüm bunu
  açıkça söyler.
- **Hatırlatma e-postaları**: her sabah 09:00'da üyeye (üyelik bitişi, haftalık değerlendirme, ölçüm) ve koça
  (ilgi bekleyenlerin özeti). SMTP yoksa **hiçbir şey gönderilmez ve gönderildi diye kaydedilmez**. Üye
  `/uye/profil`'den kapatabilir.
- **Çevrimdışı kullanım (PWA)**: üye alanı ana ekrana eklenebilir; internet yokken son yüklenen plan okunur
  ve kaydedilen antrenman telefonda bekler, bağlantı gelince kendiliğinden gönderilir. Yalnızca uygulamanın
  kendisi önbelleğe alınır, API yanıtları asla; plan önbelleği çıkışta silinir.

## Yedekleme

`yedekle.bat` (Windows) veritabanını (`pg_dump`) ve yüklenen fotoğrafları `yedekler/` klasörüne yazar;
klasör git'e girmez. **Yedekler sağlık verisi ve vücut fotoğrafı içerir** — başka bir diske ya da şifreli bir
yere kopyalayın. Geri yüklemek için (boş bir veritabanına):

```bash
docker compose exec -T postgres psql -U s2000coaching -d s2000coaching < yedekler/veritabani-TARIH.sql
```

Fotoğraf zip'ini `backend/data/photos` içine açın. Veritabanı yedeği Linux'ta alınıp ayrı bir veritabanına
geri yüklenerek denendi; `yedekle.bat`'ın Windows'a özgü kısımları (PowerShell, zip) denenmedi.

## Yapılandırma

| Ortam değişkeni | Varsayılan | Açıklama |
| --- | --- | --- |
| `ADMIN_EMAIL` | — | İlk koç hesabının e-postası. Tanımlı değilse hesap oluşturulmaz ve panele girilemez. |
| `ADMIN_PASSWORD` | — | İlk koç hesabının şifresi. Yalnızca bcrypt özeti saklanır. |
| `ADMIN_NAME` | `Koç` | Panelde görünen ad. |
| `ADMIN_SESSION_TTL` | `12h` | Yönetim oturumunun geçerlilik süresi. |
| `ADMIN_LOCKOUT_DURATION` | `15m` | 5 hatalı şifreden sonra hesabın kilitli kalma süresi. |
| `PHOTO_STORAGE_PATH` | `./data/photos` | Gelişim fotoğraflarının yazıldığı dizin. Veritabanında yalnızca anahtar tutulur. |
| `PHOTO_MAX_SIZE_BYTES` | `8388608` (8 MB) | Tek fotoğrafın üst sınırı. `spring.servlet.multipart.max-file-size` ile aynı değeri kullanır; altında kalırsa yükleme servise ulaşmadan reddedilir. |
| `PHOTO_MAX_PER_MEMBER` | `60` | Üye başına saklanan fotoğraf sayısı. Sınır olmadan tek üye herkesin diskini doldurabilir. |
| `VERIFICATION_CODE_TTL` | `15m` | Onay/giriş kodunun geçerlilik süresi. |
| `MEMBER_SESSION_TTL` | `14d` | Üye oturumunun geçerlilik süresi. |
| `MAIL_FROM` | `no-reply@s2000coaching.com` | Gönderen adresi. |
| `PUBLIC_URL` | `http://localhost:7000` | E-postalardaki bağlantılar ve ödeme dönüşü için sitenin herkese açık adresi. |
| `REMINDERS_ENABLED` / `REMINDERS_CRON` | `true` / `0 0 9 * * *` | Günlük hatırlatma işi. SMTP yoksa hiçbir şey yapmaz. |
| `PAYMENT_IBAN`, `PAYMENT_IBAN_HOLDER`, `PAYMENT_IBAN_BANK` | — | Tanımlıysa havale seçeneği açılır ve test ödemesi kapanır. |
| `PAYMENT_IYZICO_API_KEY`, `_SECRET_KEY`, `_CALLBACK_URL` | — | Üçü de tanımlıysa kart seçeneği açılır. `_BASE_URL` varsayılanı sandbox'tır. |
| `MAIL_HOST` (+ `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`) | — | SMTP sunucusu. Boşsa e-posta gönderilmez, kodlar loga yazılır. |

**Önemli:** `MAIL_HOST` tanımlı değilse e-posta gönderilmez; kodlar yalnızca uygulama loguna
`WARN` seviyesinde yazılır (`LoggingVerificationMailSender`). Bu yalnızca yerel geliştirme içindir —
prodüksiyonda SMTP ayarları mutlaka verilmelidir. Ayarların çalıştığını **Ayarlar → Test E-postası Gönder**
ile görürsünüz (kendi adresinize gider; başarısızsa mail sunucusunun verdiği neden yazar). Üye
oluştururken ya da kodu tekrar gönderirken e-posta gitmediyse üyenin sayfasında bu açıkça uyarılır;
üyenin kendi giriş kodu isteği ise adresin kayıtlı olup olmadığını sızdırmamak için sonucu söylemez.

### Üyenin kendi bilgileri

Üye `/uye/profil` sayfasından ad soyad, telefon, boy ve spor geçmişini kendisi güncelleyebilir
(`PATCH /api/member/me`). Düzenlenebilir alanlar koçun formundan dar tutulmuştur:

- **E-posta** hesabı tanımlayan ve giriş kodlarının gittiği adrestir; değişmesi kendi doğrulama
  akışını gerektirir.
- **Başlangıç kilosu** gelişim grafiklerinin çizildiği referans noktasıdır; üye değiştirebilseydi
  kendi geçmişini farkında olmadan yeniden çizerdi. Üye bugünkü kilosunu ölçüm olarak girer.

Düzenlenen üye her zaman jetondan çözülür, istek gövdesindeki bir kimlikten değil — bir üye
başkasının bilgilerini değiştiremez.

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

## Ödemeler ve üyelik süresi

Checkout, **yapılandırılmış** ödeme yöntemlerini sunar (`GET /api/payment-methods`):

| Yöntem | Ne zaman açılır | Sipariş durumu |
| --- | --- | --- |
| **IBAN / havale** | `PAYMENT_IBAN` + `PAYMENT_IBAN_HOLDER` tanımlıysa | `AWAITING_TRANSFER` → koç **Ödeme Alındı** der → `PAID` |
| **Kart (iyzico)** | `PAYMENT_IYZICO_API_KEY`, `_SECRET_KEY`, `_CALLBACK_URL` tanımlıysa | `AWAITING_PAYMENT` → iyzico geri çağırır → `PAID` ya da `FAILED` |
| Test ödemesi (mock) | **Hiçbiri tanımlı değilse** (yalnızca geliştirme) | `MOCK_PAID` — gerçek para yok |

Gerçek bir yöntem tanımlandığı anda **mock kapanır**: yapılandırılmış bir site, siparişi ödenmiş
gösteren test yöntemine yönlendirilemez.

**IBAN akışı.** Alıcı siparişten sonra IBAN'ı ve **sipariş numarasını** görür (açıklamaya yazması gereken
referans budur). Para hesaba geçince koç `/yonetim/siparisler` sayfasında **Ödeme Alındı** der; yalnızca
bu onay siparişi `PAID` yapar ve üyelik süresini başlatır. Aynı siparişi iki kez onaylamak reddedilir.

**Kart akışı.** Alıcı iyzico'nun kendi sayfasına gider — kart bilgisi bu uygulamaya hiç uğramaz. iyzico
tarayıcıyı `…/api/payments/iyzico/callback` adresine yalnızca bir **token** ile geri gönderir; token ödeme
kanıtı değildir. Sipariş, iyzico'ya **sunucudan sunucuya** sorularak çözülür ve ancak üçü birden tutarsa
`PAID` olur: yanıt gerçek (imza doğrulandı), ödeme **bu siparişin numarası** için açılmış, çekilen tutar
siparişin fiyatına eşit. Sayfa yenilenirse ikinci ziyaret hiçbir şeyi değiştirmez; iyzico'ya ulaşılamazsa
sipariş tahmin edilmeden beklemede kalır.

> **Canlıya almadan önce:** iyzico entegrasyonu bu depoda iyzico'nun kendisine karşı **denenmedi** (satıcı
> anahtarı gerekir); sonuç kuralları sahte bir istemciyle birim testlidir. `PAYMENT_IYZICO_BASE_URL`
> varsayılan olarak **sandbox**'tır (`https://sandbox-api.iyzipay.com`): sandbox anahtarlarıyla bir ödeme
> yapıp siparişin `PAID` olduğunu görün, sonra `https://api.iyzipay.com`'a geçin. `CALLBACK_URL`,
> alıcının tarayıcısının erişebildiği **herkese açık** backend adresi olmalıdır. iyzico alıcı için kimlik
> numarası ve adres ister; checkout ikisini de toplamadığından yer tutucu gönderilir
> (`PAYMENT_IYZICO_BUYER_*`). Fatura için gerçek bilgi gerekiyorsa checkout'a alan eklenmelidir.

**Üyelik süresi.** Ödenmiş bir sipariş bir `membership` dönemi doğurur (paketin ay sayısı kadar). Yeni
dönem, önceki bitmemişse **onun bitiminden** başlar — erken yenileme gün kaybettirmez. Dönem üye siparişle
bağlandığında verilir; önce ödeyip sonra üye olan biri paraladığı süreyi üye olduğu anda alır. Koç, nakit
gibi dükkân dışı ödemeler için üye detayında elle dönem ekleyebilir. Üye `/uye/profil`'de bitiş tarihini
görür; son 14 günde "Bugün" ekranında yenileme uyarısı çıkar.

## Verilerim (aylık tablolar)

Üye `/uye/verilerim` sayfasında **her ay için bir tablo** doldurur (örn. 10/2026, 11/2026): boy, kilo,
omuz, göğüs, kol, boyun, bel, kalça ve tek bacak çevresi. **Kaydet** ile ay kaydolur; kalem simgesi
tabloyu düzenler (ay değiştirilemez), çöp kutusu siler. Kurallar:

- Bir ay için **tek tablo** vardır; aynı ay tekrar eklenirse sunucu 409 döner ve kalemle düzenlemesi söylenir.
- En az bir değer zorunludur, boş bırakılan satır "ölçülmedi" demektir (—). Ondalık virgül kabul edilir.
- Gelecek bir ay girilemez.
- Veriler sağlık verisidir: üye **KVKK sağlık verisi onayını** vermeden sayfa yalnızca onay kartını gösterir ve
  sunucu satır yazmaz. Silme her zaman serbesttir; üye silinince tabloları da silinir.
- Koç, üye detayındaki **Aylık Veriler** sekmesinden aynı tabloları ekleyip düzenleyebilir ve silebilir
  (`/api/admin/members/{id}/monthly-records`). Bu, mevcut **Ölçümler** (haftalık check-in + grafik) ile
  karışmaz; ikisi ayrı tablolarda tutulur.

Koçun **Yeni Üye** formunda telefon artık isteğe bağlıdır; cinsiyet ve yaş girilebilir.

## Site ayarları (sosyal bağlantılar, rakamlar, yasal sayfalar)

Koç, **Yönetim → Ayarlar** altında herkese açık sitenin üç şeyini kod değiştirmeden düzenler
(`site_setting` tablosu; satırı olmayan ayar "ayarlanmadı" demektir, sistem boşluğu kendi uydurduğu bir
değerle doldurmaz):

- **Sosyal bağlantılar** (Instagram, YouTube, WhatsApp): sitenin altında yalnızca doldurulanlar görünür.
  Girdi doğrulanır ve normalleştirilir: Instagram kullanıcı adı ya da `instagram.com` adresi, YouTube
  `youtube.com`/`youtu.be`, WhatsApp telefon numarası (`0555…` → `https://wa.me/90555…`) ya da `wa.me`.
  Yalnızca `https` ve ilgili ağın kendi alan adı kabul edilir; üç bağlantı birlikte kontrol edilir, biri
  hatalıysa hiçbiri yazılmaz. Bağlantılar yeni sekmede `noopener noreferrer` ile açılır.
- **Ana sayfa rakamları** (kahramanın altındaki dört kart): henüz düzenlenmediyse sitenin hazır (örnek)
  değerleri görünür; koç kendi değerlerini yazar, kart siler ya da **Hazır değerlere dön** der. Kartların
  hepsi silinip kaydedilirse bölüm görünmez. Yazılan rakamların doğruluğu koçun sorumluluğundadır.
- **Yasal sayfalar** (`/yasal/kullanim-kosullari`, `/kvkk`, `/mesafeli-satis`, `/iade`): metni koç hukuk
  danışmanının onayladığı haliyle yapıştırır; sistem metin yazmaz. Yazılmamış bir sayfa "henüz
  yayınlanmadı" der, metin silinip kaydedilirse yayından kalkar. Metin düz paragraflar olarak gösterilir
  (işaretleme çalıştırılmaz). Alt bilgideki bağlantılar ve ödeme penceresindeki onay cümlesi bu sayfalara gider.

Herkese açık uçlar `GET /api/site` ve `GET /api/site/legal/{slug}`; üye verisi okumazlar.

## Tecrübelerim

Herkese açık, salt okunur bir bölüm: kategoriler ve altındaki tecrübe kartları veritabanından
(`GET /api/experience-categories`) okunur. Düzenleme uçları yoktur; içerik Liquibase seed'leriyle gelir.
Ana sayfada artık vaka çalışması bölümü yoktur; koçun kendi verisi yönetim alanında, diğer üyeler gibi
bir üye hesabı olarak tutulur (vaka çalışması uçları ve yönetim sayfaları yerinde durur).

## Testler

```bash
cd backend && ./gradlew test   # unit + @WebMvcTest + Testcontainers (Docker gerekir)
cd frontend && npm run build   # tsc + vite build
cd frontend && npm run lint    # oxlint
```

Aynı üçü her push ve pull request'te GitHub Actions'ta da koşar (`.github/workflows/ci.yml`).
Testcontainers testleri runner'ın Docker'ıyla çalışır; backend testi düşerse HTML raporu iş
çıktısı olarak yüklenir.
