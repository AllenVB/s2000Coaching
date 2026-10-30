@echo off
rem S2000 Coaching - calisan backend ve frontend'i kapatir, veritabanini durdurur.
cd /d "%~dp0"
for %%p in (7000 7001) do (
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr /r /c:":%%p .*LISTENING"') do taskkill /PID %%a /T /F >nul 2>&1
)
docker compose stop
echo.
echo Durduruldu.
pause
