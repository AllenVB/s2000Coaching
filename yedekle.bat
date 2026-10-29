@echo off
rem ===================================================================
rem  S2000 Coaching - yedek alir: veritabani + yuklenen fotograflar.
rem
rem  Kullanim: proje kokunde cift tikla, ya da CMD'de:  yedekle.bat
rem
rem  Sonuc "yedekler" klasorune yazilir:
rem    veritabani-TARIH.sql   (tum veritabani, geri yuklenebilir)
rem    fotograflar-TARIH.zip  (gelisim ve hikaye fotograflari)
rem
rem  Bu dosyalar SAGLIK VERISI ve VUCUT FOTOGRAFI icerir (KVKK'da ozel
rem  nitelikli veri). "yedekler" klasoru git'e girmez; yine de baska bir
rem  diske / sifreli bir yere kopyalayin, e-postayla ya da herkese acik
rem  bir bulutla paylasmayin.
rem ===================================================================

setlocal EnableDelayedExpansion
cd /d "%~dp0"

where docker >nul 2>&1
if errorlevel 1 (
    echo.
    echo   HATA: Docker bulunamadi. Veritabani Docker icinde calisiyor.
    goto :hata
)

set "pgcalisiyor="
for /f "delims=" %%i in ('docker compose ps --status running -q postgres 2^>nul') do set "pgcalisiyor=%%i"
if not defined pgcalisiyor (
    echo.
    echo   HATA: Veritabani konteyneri calismiyor. Once baslat.bat ile ya da
    echo         docker compose up -d  ile acin, sonra tekrar deneyin.
    goto :hata
)

for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd-HHmmss"') do set "damga=%%i"
if not exist "yedekler" mkdir "yedekler"

echo.
echo [1/2] Veritabani yedekleniyor...
docker compose exec -T postgres pg_dump -U s2000coaching -d s2000coaching --no-owner > "yedekler\veritabani-!damga!.sql"
if errorlevel 1 (
    echo.
    echo   HATA: Veritabani yedegi alinamadi. Yukaridaki mesaja bakin.
    del "yedekler\veritabani-!damga!.sql" >nul 2>&1
    goto :hata
)

rem Bos bir dosya basarili gibi gorunur ama geri yuklenemez; boyutuna bakiyoruz.
powershell -NoProfile -Command "if ((Get-Item 'yedekler\veritabani-!damga!.sql').Length -lt 1000) { exit 1 } else { exit 0 }"
if errorlevel 1 (
    echo.
    echo   HATA: Yedek dosyasi bos ya da cok kucuk; guvenilmemeli.
    del "yedekler\veritabani-!damga!.sql" >nul 2>&1
    goto :hata
)
echo       Yazildi: yedekler\veritabani-!damga!.sql

echo.
echo [2/2] Fotograflar yedekleniyor...
if exist "backend\data\photos" (
    powershell -NoProfile -Command "Compress-Archive -Path 'backend\data\photos\*' -DestinationPath 'yedekler\fotograflar-!damga!.zip' -Force"
    if errorlevel 1 (
        echo.
        echo   HATA: Fotograf yedegi alinamadi.
        goto :hata
    )
    echo       Yazildi: yedekler\fotograflar-!damga!.zip
) else (
    echo       Fotograf klasoru yok (henuz fotograf yuklenmemis); atlandi.
)

echo.
echo ====================================================
echo   Yedek tamam: %CD%\yedekler
echo.
echo   Geri yuklemek icin (bos bir veritabanina):
echo     docker compose exec -T postgres psql -U s2000coaching -d s2000coaching ^< yedekler\veritabani-TARIH.sql
echo   Fotograflari backend\data\photos klasorune acin.
echo ====================================================
echo.
pause
exit /b 0

:hata
echo.
echo Yedekleme yarida kesildi.
echo.
pause
exit /b 1
