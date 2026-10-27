@echo off
rem ===================================================================
rem  S2000 Coaching - tek komutla gelistirme ortamini ayaga kaldirir.
rem
rem  Kullanim: proje kokunde cift tikla, ya da CMD'de:  baslat.bat
rem
rem  Uc parcayi sirayla baslatir: PostgreSQL (Docker), backend ve
rem  frontend. Backend ile frontend kendi pencerelerinde acilir, cunku
rem  ikisi de calismaya devam eder; bu pencere isini bitirip kapanir.
rem ===================================================================

setlocal EnableDelayedExpansion
cd /d "%~dp0"

rem --- Ilk koc hesabinin bilgileri. Bunlar tanimli degilse yonetim paneline
rem     giris yapilamaz.
rem
rem     Gercek bir sifre bu dosyaya YAZILMAZ: bu dosya git'te takip ediliyor ve
rem     uzak depoya gidiyor. Kendi bilgilerinizi yanindaki yerel-ayarlar.bat
rem     dosyasina yazin; o dosya .gitignore'da, depoya hic cikmaz. Ornek icin
rem     yerel-ayarlar.ornek.bat dosyasina bakin.
set "ADMIN_EMAIL=koc@s2000coaching.com"
set "ADMIN_PASSWORD=Demo1234Parola"
set "ADMIN_NAME=S2000 Koc"

if exist "%~dp0yerel-ayarlar.bat" (
    call "%~dp0yerel-ayarlar.bat"
    echo       Yerel ayarlar okundu: yerel-ayarlar.bat
)

echo.
echo ====================================================
echo   S2000 Coaching - gelistirme ortami baslatiliyor
echo ====================================================
echo.

rem ---------------------------------------------------------------
rem  1) Gerekli programlar kurulu mu
rem ---------------------------------------------------------------
echo [1/4] Gerekli programlar kontrol ediliyor...

where java >nul 2>&1
if errorlevel 1 (
    echo.
    echo   HATA: Java bulunamadi. Java 21 kurup tekrar deneyin.
    echo         https://adoptium.net/temurin/releases/?version=21
    goto :hata
)

where node >nul 2>&1
if errorlevel 1 (
    echo.
    echo   HATA: Node.js bulunamadi. Node 22 kurup tekrar deneyin.
    echo         https://nodejs.org
    goto :hata
)

where docker >nul 2>&1
if errorlevel 1 (
    echo.
    echo   HATA: Docker bulunamadi. Veritabani Docker ile calisiyor.
    echo         Docker Desktop kurup acin: https://docker.com/products/docker-desktop
    goto :hata
)

docker info >nul 2>&1
if errorlevel 1 (
    echo.
    echo   HATA: Docker kurulu ama calismiyor.
    echo         Docker Desktop'i acin, balina simgesi durulana kadar bekleyin,
    echo         sonra bu dosyayi tekrar calistirin.
    goto :hata
)

echo       Java, Node ve Docker hazir.

rem --- Portlar bos mu
rem 3000 mesgulse Vite normalde sessizce bir sonraki bos porta kayar; backend'in CORS
rem listesinde yalnizca 3000 oldugu icin sayfa acilir ama hicbir veri gelmez.
rem vite.config.ts artik strictPort ile buna izin vermiyor, burada da pesinen soyluyoruz.
netstat -ano | findstr /r /c:":3000 .*LISTENING" >nul 2>&1
if not errorlevel 1 (
    echo.
    echo   HATA: 3000 portu kullanimda. Frontend baska bir porta kayamaz,
    echo         cunku backend yalnizca 3000'e izin veriyor.
    echo.
    echo         Kullanani bulmak icin:   netstat -ano ^| findstr :3000
    echo         Sonra o PID'i kapatin:   taskkill /PID ^<numara^> /F
    echo         Genelde sebebi: onceden acik kalmis bir frontend penceresi.
    goto :hata
)
netstat -ano | findstr /r /c:":8080 .*LISTENING" >nul 2>&1
if not errorlevel 1 (
    echo.
    echo   HATA: 8080 portu kullanimda; backend baslayamaz.
    echo         Kullanani bulmak icin:   netstat -ano ^| findstr :8080
    echo         Sonra o PID'i kapatin:   taskkill /PID ^<numara^> /F
    goto :hata
)
echo       3000 ve 8080 portlari bos.

rem --- 5432 (veritabani) portu
rem Bu proje icin postgres zaten calisiyorsa (ikinci kez calistirma) 5432'nin dolu
rem olmasi normaldir; o durumda kontrol atlanir, eslemeyi asagida yine dogruluyoruz.
rem Calismiyorsa 5432'yi baska bir sey tutuyorsa konteyner ya hic baslamaz ya da
rem "5432/tcp" gibi host eslemesi olmadan baslar: konteynerin ICINDE postgres calisir
rem ama Windows'taki backend ona ulasamaz. Bunu burada, baslatmadan once yakaliyoruz.
set "pgcalisiyor="
for /f "delims=" %%i in ('docker compose ps --status running -q postgres 2^>nul') do set "pgcalisiyor=%%i"
if not defined pgcalisiyor (
    netstat -ano | findstr /r /c:":5432 .*LISTENING" >nul 2>&1
    if not errorlevel 1 (
        echo.
        echo   HATA: 5432 portu baska bir sey tarafindan kullaniliyor; veritabani
        echo         konteyneri bu porta baglanamaz.
        echo.
        echo         Muhtemel sebep: baska bir Docker projesinin postgres'i ya da
        echo         bilgisayarda kurulu bir PostgreSQL 5432'yi tutuyor.
        echo         Kullanani bulmak icin:   netstat -ano ^| findstr :5432
        echo         Docker konteyneri ise:   docker ps
        echo         Cozum: o projenin klasorunde  docker compose down  calistirin,
        echo         ya da Windows PostgreSQL servisini durdurun, sonra tekrar deneyin.
        goto :hata
    )
    echo       5432 portu bos.
) else (
    echo       Bu projenin postgres konteyneri zaten calisiyor.
)

rem ---------------------------------------------------------------
rem  2) PostgreSQL
rem ---------------------------------------------------------------
echo.
echo [2/4] PostgreSQL baslatiliyor...

docker compose up -d
if errorlevel 1 (
    echo.
    echo   HATA: PostgreSQL baslatilamadi. Yukaridaki Docker mesajina bakin.
    goto :hata
)

rem Port eslemesi gercekten olustu mu? "docker compose port" eslenmemisse bos doner ya da
rem hata verir. Eslenmediyse konteyner calisir ama Windows'tan ulasilamaz (docker ps'te
rem PORTS sutununda "0.0.0.0:5432->5432/tcp" yerine yalnizca "5432/tcp" gorunur).
set "pgesleme="
for /f "delims=" %%p in ('docker compose port postgres 5432 2^>nul') do set "pgesleme=%%p"
if not defined pgesleme goto :eslemehata
echo !pgesleme! | findstr /c:":5432" >nul 2>&1
if errorlevel 1 goto :eslemehata
echo       Port eslemesi tamam: !pgesleme! -^> konteyner 5432

rem Once konteynerin icinde postgres hazir mi. Bu tek basina yetmez: konteynerin
rem icinden hazir olmasi Windows'tan ulasilabilir oldugunu kanitlamaz.
echo       Veritabaninin hazir olmasi bekleniyor...
set /a denemeler=0
:bekle
set /a denemeler+=1
docker compose exec -T postgres pg_isready -U s2000coaching -d s2000coaching >nul 2>&1
if not errorlevel 1 goto :icerihazir
if %denemeler% geq 30 (
    echo.
    echo   HATA: PostgreSQL 60 saniyede hazir olmadi.
    echo         Kayitlari gormek icin:  docker compose logs postgres
    goto :hata
)
timeout /t 2 /nobreak >nul
goto :bekle

:icerihazir
rem Sonra Windows'tan localhost:5432'ye gercekten baglanilabiliyor mu: backend'in
rem gorecegi yol bu. psql gerekmez; PowerShell ile TCP baglantisi denenir.
echo       localhost:5432 Windows tarafindan kontrol ediliyor...
set /a tdeneme=0
:tbekle
set /a tdeneme+=1
powershell -NoProfile -Command "if ((Test-NetConnection -ComputerName localhost -Port 5432 -InformationLevel Quiet -WarningAction SilentlyContinue)) { exit 0 } else { exit 1 }" >nul 2>&1
if not errorlevel 1 goto :hazir
if %tdeneme% geq 10 (
    echo.
    echo   HATA: PostgreSQL konteynerde calisiyor ama localhost:5432'ye Windows'tan
    echo         baglanilamadi. Backend de baglanamaz.
    echo         Kontrol edin:  docker ps   ^(PORTS sutununda 0.0.0.0:5432-^>5432/tcp olmali^)
    echo         Baska bir proje 5432'yi tutuyorsa onun klasorunde  docker compose down
    echo         calistirin, sonra bu projede  docker compose up -d  deneyin.
    goto :hata
)
timeout /t 2 /nobreak >nul
goto :tbekle

:eslemehata
echo.
echo   HATA: PostgreSQL konteyneri calisiyor ama 5432 portu Windows'a acilmamis.
echo         Muhtemel sebep: baska bir Docker projesi ^(ornegin routewise^) 5432'yi
echo         tutuyor. docker ps'te PORTS sutunu yalnizca 5432/tcp gosterir.
echo         Cozum: o projenin klasorunde  docker compose down  calistirin,
echo         sonra bu projede  docker compose down  ve  docker compose up -d.
goto :hata

:hazir
echo       PostgreSQL hazir ve localhost:5432 uzerinden ulasilabilir.

rem ---------------------------------------------------------------
rem  3) Backend
rem ---------------------------------------------------------------
echo.
echo [3/4] Backend baslatiliyor (kendi penceresinde)...

rem Ilk calistirmada Gradle bagimliliklari indirir, birkac dakika surebilir.
rem start /D ile calisma dizini verilir; boylece komut icinde ic ice tirnak
rem olmaz. Ic ice tirnaklari CMD tutarsiz yorumlar ve yolda bosluk varsa kirilir.
rem "set X=%X%&&" arasinda bosluk yok: olsaydi degere bosluk eklenirdi.
start "S2000 Backend" /D "%~dp0backend" cmd /k "set ADMIN_EMAIL=%ADMIN_EMAIL%&& set ADMIN_PASSWORD=%ADMIN_PASSWORD%&& set ADMIN_NAME=%ADMIN_NAME%&& gradlew bootRun"

echo       Backend penceresi acildi. "Started BackendApplication" yazisini bekleyin.

rem ---------------------------------------------------------------
rem  4) Frontend
rem ---------------------------------------------------------------
echo.
echo [4/4] Frontend baslatiliyor (kendi penceresinde)...

rem node_modules yoksa once kurulum yap; varsa dogrudan baslat.
if exist "%~dp0frontend\node_modules" (
    start "S2000 Frontend" /D "%~dp0frontend" cmd /k "npm run dev"
) else (
    echo       Ilk calistirma: npm paketleri kuruluyor, birkac dakika surebilir.
    start "S2000 Frontend" /D "%~dp0frontend" cmd /k "npm install && npm run dev"
)

echo       Frontend penceresi acildi.

rem ---------------------------------------------------------------
echo.
echo ====================================================
echo   Hazir. Adresler:
echo.
echo     Tanitim sitesi  :  http://localhost:3000
echo     Uye alani       :  http://localhost:3000/uye
echo     Yonetim paneli  :  http://localhost:3000/yonetim
echo.
echo   Yonetim girisi:
echo     E-posta : %ADMIN_EMAIL%
echo     Sifre   : %ADMIN_PASSWORD%
echo.
echo   NOT: SMTP tanimli olmadigi icin uyelere dogrulama kodu
echo        e-postayla GITMEZ. Kod, backend penceresinde WARN
echo        satiri olarak yazilir; oradan okuyup girin.
echo.
echo   Durdurmak icin: iki pencereyi kapatin, sonra  docker compose down
echo ====================================================
echo.
rem Sabit sure beklemek yerine frontend gercekten cevap verene kadar yokla:
rem ilk calistirmada npm install dakikalar surebilir, hazir derlenmisse birkac
rem saniye. Windows 10 ve sonrasinda curl kurulu gelir.
echo Frontend hazir olunca tarayici acilacak...
set /a fdeneme=0
:fbekle
set /a fdeneme+=1
curl -s -o nul http://localhost:3000 >nul 2>&1
if not errorlevel 1 goto :fhazir
if %fdeneme% geq 150 (
    echo   Frontend 5 dakikada acilmadi. Frontend penceresindeki mesajlara bakin.
    goto :son
)
timeout /t 2 /nobreak >nul
goto :fbekle

:fhazir
start "" http://localhost:3000
goto :son

:hata
echo.
echo Baslatma yarida kesildi.
echo.
pause
exit /b 1

:son
echo Bu pencere kapatilabilir. Backend ve frontend kendi pencerelerinde calisiyor.
pause
exit /b 0
