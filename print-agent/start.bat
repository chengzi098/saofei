@echo off
chcp 65001 >nul 2>&1
title Print Agent
echo ==============================
echo   CX Printer Agent
echo ==============================
echo.

cd /d "%~dp0"

if not exist "node_modules" (
    echo First run, installing dependencies...
    npm install
    echo.
)

if "%PRINT_AGENT_KEY%"=="" (
    echo PRINT_AGENT_KEY is required.
    echo Example: set PRINT_AGENT_KEY=replace-with-a-long-random-value
    pause
    exit /b 1
)

node index.js
pause
