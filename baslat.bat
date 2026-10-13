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

rem --- Ilk kurulumda degistirilecek yer: ilk koc hesabinin bilgileri.
rem     Bunlar tanimli degilse yonetim paneline giris yapilamaz.
set "ADMIN_EMAIL=koc@s2000coaching.com"
set "ADMIN_PASSWORD=Demo1234Parola"
set "ADMIN_NAME=S2000 Koc"

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
rem 5173 mesgulse Vite normalde sessizce 5174'e kayar; backend'in CORS listesinde
rem yalnizca 5173 oldugu icin sayfa acilir ama hicbir veri gelmez. vite.config.ts
rem artik strictPort ile buna izin vermiyor, burada da pesinen sebebini soyluyoruz.
netstat -ano | findstr /r /c:":5173 .*LISTENING" >nul 2>&1
if not errorlevel 1 (
    echo.
    echo   HATA: 5173 portu kullanimda. Frontend baska bir porta kayamaz,
    echo         cunku backend yalnizca 5173'e izin veriyor.
    echo.
    echo         Kullanani bulmak icin:   netstat -ano ^| findstr :5173
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
echo       5173 ve 8080 portlari bos.

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

rem Veritabani baglanti kabul edene kadar bekle. Backend hazir olmayan bir
rem veritabanina baglanmaya calisirsa Liquibase hata verip kapanir.
echo       Veritabaninin hazir olmasi bekleniyor...
set /a denemeler=0
:bekle
set /a denemeler+=1
docker compose exec -T postgres pg_isready -U s2000coaching -d s2000coaching >nul 2>&1
if not errorlevel 1 goto :hazir
if %denemeler% geq 30 (
    echo.
    echo   HATA: PostgreSQL 60 saniyede hazir olmadi.
    echo         Kayitlari gormek icin:  docker compose logs postgres
    goto :hata
)
timeout /t 2 /nobreak >nul
goto :bekle

:hazir
echo       PostgreSQL hazir.

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
echo     Tanitim sitesi  :  http://localhost:5173
echo     Uye alani       :  http://localhost:5173/uye
echo     Yonetim paneli  :  http://localhost:5173/yonetim
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
curl -s -o nul http://localhost:5173 >nul 2>&1
if not errorlevel 1 goto :fhazir
if %fdeneme% geq 150 (
    echo   Frontend 5 dakikada acilmadi. Frontend penceresindeki mesajlara bakin.
    goto :son
)
timeout /t 2 /nobreak >nul
goto :fbekle

:fhazir
start "" http://localhost:5173
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
