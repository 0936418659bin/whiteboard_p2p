@echo off
REM Download FlatLaf library for P2P Whiteboard

setlocal enabledelayedexpansion

set "libDir=%~dp0lib"
set "flatlafUrl=https://github.com/JFormDesigner/FlatLaf/releases/download/v3.2.5/flatlaf-3.2.5.jar"
set "flatLafFile=%libDir%\flatlaf-3.2.5.jar"

echo.
echo ========================================
echo   Downloading FlatLaf Library
echo ========================================
echo.

REM Create lib directory if not exists
if not exist "%libDir%" (
    mkdir "%libDir%"
    echo Created %libDir%
)

REM Check if file already exists
if exist "%flatLafFile%" (
    echo FlatLaf JAR already exists: %flatLafFile%
    echo Skipping download
    exit /b 0
)

REM Download using PowerShell
echo Downloading FlatLaf 3.2.5...
powershell -Command "(New-Object System.Net.ServicePointManager).SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; (New-Object System.Net.WebClient).DownloadFile('%flatlafUrl%', '%flatLafFile%')"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Download completed!
    echo FlatLaf JAR saved to: %flatLafFile%
    
    REM Verify file size
    if exist "%flatLafFile%" (
        for %%f in ("%flatLafFile%") do set "size=%%~zf"
        echo File size: %size% bytes
    )
    exit /b 0
) else (
    echo.
    echo Download failed!
    echo.
    echo Manual download:
    echo 1. Go to: https://github.com/JFormDesigner/FlatLaf/releases/tag/v3.2.5
    echo 2. Download flatlaf-3.2.5.jar
    echo 3. Save to: %libDir%\flatlaf-3.2.5.jar
    pause
    exit /b 1
)
