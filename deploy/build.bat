@echo off
echo ==============================
echo   Garment System - Build
echo ==============================

set PROJECT_DIR=%~dp0..

echo.
echo [1/4] Build Backend...
cd /d "%PROJECT_DIR%\garment-server"
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo Backend build failed!
    pause
    exit /b 1
)
echo Backend build done.

echo.
echo [2/4] Build Frontend...
cd /d "%PROJECT_DIR%\garment-web"
call npm install
call npm run build
if %errorlevel% neq 0 (
    echo Frontend build failed!
    pause
    exit /b 1
)
echo Frontend build done.

echo.
echo [3/4] Prepare files...
mkdir "%PROJECT_DIR%\deploy\upload" 2>nul
copy "%PROJECT_DIR%\garment-server\target\garment-server-0.0.1-SNAPSHOT.jar" "%PROJECT_DIR%\deploy\upload\garment-server.jar"
xcopy /E /I /Y "%PROJECT_DIR%\garment-web\dist" "%PROJECT_DIR%\deploy\upload\dist"
copy "%PROJECT_DIR%\garment-server\src\main\resources\db\schema.sql" "%PROJECT_DIR%\deploy\upload\schema.sql"
copy "%PROJECT_DIR%\garment-server\src\main\resources\db\migration.sql" "%PROJECT_DIR%\deploy\upload\migration.sql"

echo.
echo [4/4] Create zip...
cd "%PROJECT_DIR%\deploy\upload"
powershell Compress-Archive -Path * -DestinationPath "%PROJECT_DIR%\deploy\garment-deploy.zip" -Force

echo.
echo ==============================
echo   Build Success!
echo ==============================
echo.
echo File: deploy\garment-deploy.zip
echo.
echo Next steps:
echo   1. Upload garment-deploy.zip to server
echo   2. Run: sudo bash deploy.sh
echo.
pause
