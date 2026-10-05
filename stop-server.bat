@echo off
echo ========================================
echo   Garment System - Stop Services
echo ========================================
echo.

echo [Stopping Backend...]
taskkill /f /fi "WindowTitle eq Garment Server*" >nul 2>&1
taskkill /f /fi "WindowTitle eq *spring-boot:run*" >nul 2>&1

echo [Stopping Frontend...]
taskkill /f /fi "WindowTitle eq Garment Web*" >nul 2>&1
taskkill /f /fi "WindowTitle eq *npm*" >nul 2>&1

echo.
echo   Services stopped!
echo.
pause
