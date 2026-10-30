@echo off
rem S2000 Coaching - gelistirme veritabanini SIFIRLAR: tum uyeler, planlar, olcumler silinir.
cd /d "%~dp0"
echo.
echo   DIKKAT: Bu bilgisayardaki gelistirme veritabani tamamen silinecek.
echo   Uyeler, planlar, olcumler, siparisler... hepsi gider. Geri alinamaz.
echo   ^(Yukledigi fotograflar backend\data\photos klasorunde kalir.^)
echo.
set /p onay=Devam etmek icin EVET yazin: 
if /i not "%onay%"=="EVET" (
    echo Vazgecildi.
    pause
    exit /b 1
)
docker compose down -v
echo.
echo Sifirlandi. baslat.bat bos bir veritabaniyla yeniden kurar.
pause
