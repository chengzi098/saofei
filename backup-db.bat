@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ============================
:: 制衣厂管理系统 - 数据库自动备份脚本
:: ============================

:: 配置区（根据实际情况修改）
set DB_USER=root
if "%DB_PASSWORD%"=="" (
    echo [错误] 请先设置环境变量 DB_PASSWORD
    exit /b 1
)
set DB_NAME=garment_db
set BACKUP_DIR=D:\backup\garment_db

:: 生成日期格式：20260604
set YEAR=%date:~0,4%
set MONTH=%date:~5,2%
set DAY=%date:~8,2%
set DATE_STR=%YEAR%%MONTH%%DAY%

:: 创建备份目录（如果不存在）
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

:: 备份文件名
set BACKUP_FILE=%BACKUP_DIR%\%DB_NAME%_%DATE_STR%.sql

:: 压缩备份文件名（需要 gzip 或 7z）
set BACKUP_FILE_GZ=%BACKUP_FILE%.gz

echo ========================================
echo   制衣厂管理系统 - 数据库备份
echo   %date% %time%
echo ========================================
echo.

:: 执行备份
echo [1/2] 正在备份数据库 %DB_NAME%...
mysqldump -u%DB_USER% -p%DB_PASSWORD% --single-transaction --routines --triggers %DB_NAME% > "%BACKUP_FILE%"

if %errorlevel% neq 0 (
    echo [错误] 备份失败！请检查 MySQL 配置
    pause
    exit /b 1
)

echo [2/2] 备份完成: %BACKUP_FILE%

:: 删除 7 天前的备份（可选）
echo.
echo 清理 7 天前的旧备份...
forfiles /p "%BACKUP_DIR%" /m *.sql /d -7 /c "cmd /c del @path" 2>nul
forfiles /p "%BACKUP_DIR%" /m *.gz /d -7 /c "cmd /c del @path" 2>nul

echo.
echo ========================================
echo   备份完成！文件保存在: %BACKUP_DIR%
echo ========================================
echo.
