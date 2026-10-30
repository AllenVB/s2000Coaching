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
rem 7000 mesgulse Vite normalde sessizce bir sonraki bos porta kayar; backend'in CORS
rem listesinde yalnizca 7000 oldugu icin sayfa acilir ama hicbir veri gelmez.
rem vite.config.ts artik strictPort ile buna izin vermiyor, burada da pesinen soyluyoruz.
netstat -ano | findstr /r /c:":7000 .*LISTENING" >nul 2>&1
if not errorlevel 1 (
    echo.
    echo   HATA: 7000 portu kullanimda. Frontend baska bir porta kayamaz,
    echo         cunku backend yalnizca 7000'e izin veriyor.
    echo.
    echo         Kullanani bulmak icin:   netstat -ano ^| findstr :7000
    echo         Sonra o PID'i kapatin:   taskkill /PID ^<numara^> /F
    echo         Genelde sebebi: onceden acik kalmis bir frontend penceresi.
    goto :hata
)
netstat -ano | findstr /r /c:":7001 .*LISTENING" >nul 2>&1
if not errorlevel 1 (
    echo.
    echo   HATA: 7001 portu kullanimda; backend baslayamaz.
    echo         Kullanani bulmak icin:   netstat -ano ^| findstr :7001
    echo         Sonra o PID'i kapatin:   taskkill /PID ^<numara^> /F
    goto :hata
)
rem --- Windows'un kendi ayirdigi port araliklari
rem Hyper-V/WSL/Docker bazi araliklari (ornegin 7000-7099) rezerve eder. Bu portlar netstat'ta
rem "bos" gorunur ama hicbir program onlara baglanamaz; Docker "bind: access permissions" hatasiyla
rem durur ve kurulum yarida kesilir. Rezerve araliklari burada okuyup once uyariyoruz.
for /f "tokens=1,2" %%a in ('netsh interface ipv4 show excludedportrange protocol=tcp 2^>nul ^| findstr /r /c:"^ *[0-9][0-9]* *[0-9][0-9]*"') do (
    for %%p in (7000 7001 7002) do (
        if %%p geq %%a if %%p leq %%b (
            echo.
            echo   HATA: %%p portu Windows tarafindan rezerve edilmis ^(%%a-%%b araligi^).
            echo         Hicbir program bu porta baglanamaz.
            echo         Cozum: yonetici CMD'de  net stop winnat  calistirin, sonra  net start winnat
            echo         ya da portlari baska bir araliga tasiyin ^(docker-compose.yml, vite.config.ts,
            echo         application.yml, frontend\.env.example^).
            goto :hata
        )
    )
)
echo       7000 ve 7001 portlari bos.

rem --- 7002 (veritabani) portu
rem Bu proje icin postgres zaten calisiyorsa (ikinci kez calistirma) 7002 portunun dolu
rem olmasi normaldir; o durumda kontrol atlanir, eslemeyi asagida yine dogruluyoruz.
rem Calismiyorsa 7002 portunu baska bir sey tutuyorsa konteyner ya hic baslamaz ya da
rem "5432/tcp" gibi host eslemesi olmadan baslar: konteynerin ICINDE postgres calisir
rem ama Windows'taki backend ona ulasamaz. Bunu burada, baslatmadan once yakaliyoruz.
set "pgcalisiyor="
for /f "delims=" %%i in ('docker compose ps --status running -q postgres 2^>nul') do set "pgcalisiyor=%%i"
if not defined pgcalisiyor (
    netstat -ano | findstr /r /c:":7002 .*LISTENING" >nul 2>&1
    if not errorlevel 1 (
        echo.
        echo   HATA: 7002 portu baska bir sey tarafindan kullaniliyor; veritabani
        echo         konteyneri bu porta baglanamaz.
        echo.
        echo         Muhtemel sebep: baska bir Docker projesinin postgres'i ya da
        echo         bilgisayarda kurulu bir PostgreSQL 7002 portunu tutuyor.
        echo         Kullanani bulmak icin:   netstat -ano ^| findstr :7002
        echo         Docker konteyneri ise:   docker ps
        echo         Cozum: o projenin klasorunde  docker compose down  calistirin,
        echo         ya da Windows PostgreSQL servisini durdurun, sonra tekrar deneyin.
        goto :hata
    )
    echo       7002 portu bos.
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
rem PORTS sutununda "0.0.0.0:7002->5432/tcp" yerine yalnizca "5432/tcp" gorunur).
set "pgesleme="
for /f "delims=" %%p in ('docker compose port postgres 5432 2^>nul') do set "pgesleme=%%p"
if not defined pgesleme goto :eslemehata
echo !pgesleme! | findstr /c:":7002" >nul 2>&1
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
rem Sonra Windows'tan localhost:7002 adresine gercekten baglanilabiliyor mu: backend'in
rem gorecegi yol bu. psql gerekmez; PowerShell ile TCP baglantisi denenir.
echo       localhost:7002 Windows tarafindan kontrol ediliyor...
set /a tdeneme=0
:tbekle
set /a tdeneme+=1
powershell -NoProfile -Command "if ((Test-NetConnection -ComputerName localhost -Port 7002 -InformationLevel Quiet -WarningAction SilentlyContinue)) { exit 0 } else { exit 1 }" >nul 2>&1
if not errorlevel 1 goto :hazir
if %tdeneme% geq 10 (
    echo.
    echo   HATA: PostgreSQL konteynerde calisiyor ama localhost:7002 adresine Windows'tan
    echo         baglanilamadi. Backend de baglanamaz.
    echo         Kontrol edin:  docker ps   ^(PORTS sutununda 0.0.0.0:7002-^>5432/tcp olmali^)
    echo         Baska bir proje 7002 portunu tutuyorsa onun klasorunde  docker compose down
    echo         calistirin, sonra bu projede  docker compose up -d  deneyin.
    goto :hata
)
timeout /t 2 /nobreak >nul
goto :tbekle

:eslemehata
echo.
echo   HATA: PostgreSQL konteyneri calisiyor ama 7002 portu Windows'a acilmamis.
echo         Muhtemel sebep: baska bir Docker projesi ^(ornegin routewise^) 7002 portunu
echo         tutuyor. docker ps'te PORTS sutunu yalnizca 5432/tcp gosterir.
echo         Cozum: o projenin klasorunde  docker compose down  calistirin,
echo         sonra bu projede  docker compose down  ve  docker compose up -d.
goto :hata

:hazir
echo       PostgreSQL hazir ve localhost:7002 uzerinden ulasilabilir.

rem ---------------------------------------------------------------
rem  3) Backend
rem ---------------------------------------------------------------
echo.
echo [3/4] Backend ve frontend arka planda baslatiliyor...

rem Iki program bu pencerenin ICINDE, arka planda calisir (start /B); pencere acik kaldigi surece
rem calisirlar. Ciktilari ekrana degil loglar\ klasorundeki dosyalara yazilir: bir sey ters giderse
rem mesaj kaybolmaz, burada son satirlari gosteririz.
rem Ilk calistirmada Gradle bagimliliklari indirir, birkac dakika surebilir.
rem ADMIN_* degiskenleri yukarida bu pencerede tanimlandi; arka plandaki programlar onlari miras alir.
if not exist "%~dp0loglar" mkdir "%~dp0loglar"
set "BACKLOG=%~dp0loglar\backend.log"
set "FRONTLOG=%~dp0loglar\frontend.log"
del "%BACKLOG%" "%FRONTLOG%" >nul 2>&1

start "" /B /D "%~dp0backend" cmd /c gradlew.bat bootRun > "%BACKLOG%" 2>&1
echo       Backend baslatildi  ^(gunluk: loglar\backend.log^)

echo.
echo [4/4] Frontend baslatiliyor...
if exist "%~dp0frontend\node_modules" (
    start "" /B /D "%~dp0frontend" cmd /c npm run dev > "%FRONTLOG%" 2>&1
) else (
    echo       Ilk calistirma: npm paketleri kuruluyor, birkac dakika surebilir.
    start "" /B /D "%~dp0frontend" cmd /c npm install ^&^& npm run dev > "%FRONTLOG%" 2>&1
)
echo       Frontend baslatildi ^(gunluk: loglar\frontend.log^)

rem ---------------------------------------------------------------
rem  Hazir olmalarini bekle
rem ---------------------------------------------------------------
echo.
echo Backend'in hazir olmasi bekleniyor ^(ilk seferde birkac dakika surebilir^)...
set /a bdeneme=0
:bbekle
set /a bdeneme+=1
curl -s -o nul http://localhost:7001/api/packages >nul 2>&1
if not errorlevel 1 goto :bhazir
findstr /c:"APPLICATION FAILED" /c:"BUILD FAILED" /c:"FAILURE: Build failed" "%BACKLOG%" >nul 2>&1
if not errorlevel 1 goto :backhata
if %bdeneme% geq 300 goto :backzaman
timeout /t 2 /nobreak >nul
goto :bbekle

:bhazir
echo       Backend hazir.
echo Frontend'in hazir olmasi bekleniyor...
set /a fdeneme=0
:fbekle
set /a fdeneme+=1
curl -s -o nul http://localhost:7000 >nul 2>&1
if not errorlevel 1 goto :fhazir
findstr /c:"is already in use" /c:"npm ERR!" "%FRONTLOG%" >nul 2>&1
if not errorlevel 1 goto :fronthata
if %fdeneme% geq 150 goto :fronthata
timeout /t 2 /nobreak >nul
goto :fbekle

:fhazir
echo       Frontend hazir.
echo.
echo ====================================================
echo   Hazir. Adresler:
echo.
echo     Tanitim sitesi  :  http://localhost:7000
echo     Uye alani       :  http://localhost:7000/uye
echo     Yonetim paneli  :  http://localhost:7000/yonetim
echo.
echo   Yonetim girisi:
echo     E-posta : %ADMIN_EMAIL%
echo     Sifre   : %ADMIN_PASSWORD%
echo.
echo   NOT: SMTP tanimli olmadigi icin uyelere dogrulama kodu
echo        e-postayla GITMEZ. Kod, loglar\backend.log icinde WARN
echo        satiri olarak yazilir; oradan okuyup girin.
echo.
echo   Gunlukler: loglar\backend.log ve loglar\frontend.log
echo ====================================================
echo.
start "" http://localhost:7000
echo Kapatmak icin bu pencerede bir tusa basin. ^(Pencereyi kapatmak da durdurur.^)
pause >nul
call :durdur
echo.
echo Durduruldu. Veritabani calismaya devam ediyor; onu da kapatmak icin:  docker compose stop
pause
exit /b 0

rem ---------------------------------------------------------------
:backzaman
echo.
echo   HATA: Backend 10 dakikada hazir olmadi.
goto :backgoster

:backhata
echo.
echo   HATA: Backend baslarken hata verdi.
:backgoster
echo         Son satirlar ^(tamami: loglar\backend.log^):
echo ----------------------------------------------------
powershell -NoProfile -Command "Get-Content -Tail 30 -LiteralPath '%BACKLOG%'"
echo ----------------------------------------------------
call :durdur
goto :hata

:fronthata
echo.
echo   HATA: Frontend baslamadi. Son satirlar ^(tamami: loglar\frontend.log^):
echo ----------------------------------------------------
powershell -NoProfile -Command "Get-Content -Tail 30 -LiteralPath '%FRONTLOG%'"
echo ----------------------------------------------------
call :durdur
goto :hata

:hata
echo.
echo Baslatma yarida kesildi. Yukaridaki HATA satirini ve gunluk cikisini bana gonderin.
echo.
pause
exit /b 1

rem 7000 ve 7001 portunu dinleyen programlari agaciyla birlikte kapatir.
:durdur
for %%p in (7000 7001) do (
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr /r /c:":%%p .*LISTENING"') do taskkill /PID %%a /T /F >nul 2>&1
)
exit /b 0
