@echo off
rem ===================================================================
rem  Ornek yerel ayar dosyasi.
rem
rem  Kullanim: bu dosyayi "yerel-ayarlar.bat" adiyla kopyalayin ve kendi
rem  bilgilerinizi yazin. baslat.bat varsa onu okur, yoksa kendi
rem  varsayilanlariyla devam eder.
rem
rem  "yerel-ayarlar.bat" .gitignore'da oldugu icin depoya cikmaz; gercek
rem  sifreniz yalnizca bu bilgisayarda kalir. Ornek dosyaya gercek sifre
rem  yazmayin.
rem ===================================================================

set "ADMIN_EMAIL=ornek@ornek.com"
set "ADMIN_PASSWORD=BurayaKendiSifreniz"
set "ADMIN_NAME=Ad Soyad"

rem     Bu hesap, veritabaninda yoksa baslarken olusturulur (daha once baska bir koc hesabi
rem     olsa bile). Varsa dokunulmaz: sifreyi sonradan Ayarlar sayfasindan degistirdiysen
rem     buradaki degisikligin etkisi olmaz. En az 10 karakter. Bu dosyayi kimseyle paylasma.

rem --- E-posta gonderimi (istege bagli).
rem     MAIL_HOST tanimli degilse dogrulama kodlari e-posta ile GITMEZ, backend
rem     penceresine WARN satiri olarak yazilir; gelistirme icin istenen budur.
rem     Asagidakileri doldurursan kodlar gercekten postalanir.
rem     Gmail icin: host smtp.gmail.com, port 587, sifre olarak hesabin normal
rem     sifresi degil "uygulama sifresi" kullanilir.
rem set "MAIL_HOST=smtp.gmail.com"
rem set "MAIL_PORT=587"
rem set "MAIL_USERNAME=ornek@gmail.com"
rem set "MAIL_PASSWORD=uygulama-sifresi"
rem set "MAIL_FROM=S2000 Coaching <ornek@gmail.com>"

rem --- Odeme: IBAN / havale (istege bagli).
rem     IBAN ve hesap sahibi tanimlanirsa odeme sayfasi havale secenegini sunar ve
rem     test odemesi KAPANIR: siparis "Havale bekleniyor" olarak durur, parayi
rem     gordugunuzde yonetim panelindeki Siparisler sayfasindan "Odeme Alindi"
rem     dersiniz ve uyelik suresi o anda baslar. Tanimli degilse siparisler test
rem     modunda aninda onaylanir (yalnizca gelistirme icin).
rem set "PAYMENT_IBAN=TR00 0000 0000 0000 0000 0000 00"
rem set "PAYMENT_IBAN_HOLDER=Ad Soyad"
rem set "PAYMENT_IBAN_BANK=Banka Adi"

rem --- Hatirlatma e-postalari.
rem     SMTP tanimliysa her sabah 09:00'da uyelere (uyelik bitisi, haftalik degerlendirme,
rem     olcum) ve size (ilgi bekleyenlerin ozeti) e-posta gider. SMTP tanimli degilse hicbir
rem     sey gonderilmez ve gonderildi diye de kaydedilmez. E-postalardaki baglantilar icin
rem     sitenizin adresini yazin:
rem set "PUBLIC_URL=https://siteniz.com"
