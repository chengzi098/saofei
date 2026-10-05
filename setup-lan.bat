@echo off
echo ========================================
echo   Garment System - LAN Setup
echo ========================================
echo.

net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERROR] Please run as Administrator!
    echo Right-click this file and select "Run as administrator"
    pause
    exit /b 1
)

echo [1/3] Opening firewall ports...
echo.

netsh advfirewall firewall show rule name="Garment Web 5173" >nul 2>&1
if %errorLevel% equ 0 (
    echo   - Port 5173 already open, skip
) else (
    netsh advfirewall firewall add rule name="Garment Web 5173" dir=in action=allow protocol=tcp localport=5173 >nul
    echo   + Port 5173 opened
)

netsh advfirewall firewall show rule name="Garment Server 8080" >nul 2>&1
if %errorLevel% equ 0 (
    echo   - Port 8080 already open, skip
) else (
    netsh advfirewall firewall add rule name="Garment Server 8080" dir=in action=allow protocol=tcp localport=8080 >nul
    echo   + Port 8080 opened
)

netsh advfirewall firewall show rule name="MySQL 3306" >nul 2>&1
if %errorLevel% equ 0 (
    echo   - Port 3306 already open, skip
) else (
    netsh advfirewall firewall add rule name="MySQL 3306" dir=in action=allow protocol=tcp localport=3306 >nul
    echo   + Port 3306 opened
)

echo.
echo [2/3] Getting local IP address...
echo.

for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /i "IPv4" ^| findstr /v "127.0.0.1"') do (
    set "IP=%%a"
    set "IP=!IP: =!"
)

if defined IP (
    echo   Local IP: !IP!
) else (
    echo   [WARNING] Cannot find local IP, run ipconfig manually
    set "IP=YOUR_IP"
)

echo.
echo [3/3] MySQL Remote Access
echo.
echo   Run this SQL in MySQL client:
echo.
echo   CREATE USER IF NOT EXISTS 'garment'@'%%' IDENTIFIED BY '请替换为强密码';
echo   GRANT ALL PRIVILEGES ON garment_db.* TO 'root'@'%%';
echo   FLUSH PRIVILEGES;
echo.

echo ========================================
echo   LAN Access URLs
echo ========================================
echo.
echo   Frontend: http://!IP!:5173
echo   Backend:  http://!IP!:8080
echo.
echo   Open browser on other devices:
echo   http://!IP!:5173
echo ========================================
echo.

pause
