@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ============================
:: 制衣厂管理系统 - 数据库恢复脚本
:: ============================

set DB_USER=root
if "%DB_PASSWORD%"=="" (
    echo [错误] 请先设置环境变量 DB_PASSWORD
    exit /b 1
)
set DB_NAME=garment_db
set BACKUP_DIR=D:\backup\garment_db

echo ========================================
echo   制衣厂管理系统 - 数据库恢复
echo   %date% %time%
echo ========================================
echo.

:: 列出可用的备份文件
echo 可用的备份文件：
echo ----------------------------
set count=0
for %%f in ("%BACKUP_DIR%\*.sql") do (
    set /a count+=1
    set "file_!count!=%%f"
    echo   !count!. %%~nxf
)

if %count%==0 (
    echo [错误] 没有找到备份文件！
    echo 请先运行 backup-db.bat 创建备份
    pause
    exit /b 1
)

echo.
set /p choice="请输入要恢复的文件编号 (1-%count%): "

if !choice! lss 1 (
    echo [错误] 无效的选择
    pause
    exit /b 1
)
if !choice! gtr %count% (
    echo [错误] 无效的选择
    pause
    exit /b 1
)

set RESTORE_FILE=!file_%choice%!

echo.
echo 即将恢复: %RESTORE_FILE%
echo 数据库: %DB_NAME%
echo.
echo [警告] 这将覆盖当前数据库中的所有数据！
set /p confirm="确认恢复？(输入 YES 继续): "

if not "%confirm%"=="YES" (
    echo 已取消恢复操作
    pause
    exit /b 0
)

echo.
echo [1/2] 正在恢复数据库...
mysql -u%DB_USER% -p%DB_PASSWORD% %DB_NAME% < "%RESTORE_FILE%"

if %errorlevel% neq 0 (
    echo [错误] 恢复失败！请检查备份文件和 MySQL 配置
    pause
    exit /b 1
)

echo [2/2] 恢复完成！
echo.
echo ========================================
echo   数据库已成功恢复
echo ========================================
echo.
pause
