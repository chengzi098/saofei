@echo off
setlocal enabledelayedexpansion

if exist "%~dp0.env" (
    for /f "usebackq tokens=1,* delims==" %%a in ("%~dp0.env") do (
        set "line=%%a"
        if not "!line:~0,1!"=="#" if not "%%a"=="" set "%%a=%%b"
    )
)

echo ========================================
echo   Garment System - Start Services
echo ========================================
echo.

set "IP="
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /i "IPv4" ^| findstr /v "127.0.0.1"') do (
    set "raw=%%a"
    set "IP=!raw: =!"
)

if "!IP!"=="" set "IP=localhost"

echo   Local IP: !IP!
echo   Access URL: http://!IP!:5173
echo.
echo ========================================
echo.

echo [Starting Backend...]
start "Garment Server" cmd /k "cd /d %~dp0garment-server && mvn spring-boot:run"

echo [Waiting for backend...]
timeout /t 10 /nobreak >nul

echo [Starting Frontend...]
start "Garment Web" cmd /k "cd /d %~dp0garment-web && npm run dev"

echo.
echo   Services started!
echo   Open: http://!IP!:5173
echo.
pause