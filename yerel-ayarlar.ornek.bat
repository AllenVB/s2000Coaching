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
