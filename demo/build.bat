@echo off
REM P2P Whiteboard - Build and Run Script for Windows

setlocal enabledelayedexpansion

echo.
echo ========================================
echo   P2P Whiteboard - Build Script
echo ========================================
echo.

REM Check if Maven is installed
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Error: Maven is not installed or not in PATH
    echo Please install Maven from https://maven.apache.org/
    pause
    exit /b 1
)

REM Check if Java is installed
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Error: Java is not installed or not in PATH
    echo Please install Java 17 or higher from https://jdk.java.net/
    pause
    exit /b 1
)

REM Display versions
echo Checking environment...
echo.
java -version
echo.
mvn --version
echo.

REM Parse command line arguments
if "%1"=="" (
    set ACTION=build
) else (
    set ACTION=%1
)

if "%ACTION%"=="build" goto build
if "%ACTION%"=="run" goto run
if "%ACTION%"=="clean" goto clean
if "%ACTION%"=="test" goto test
if "%ACTION%"=="package" goto package
if "%ACTION%"=="help" goto help

:build
echo Building the project...
call mvn clean package
if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    pause
    exit /b 1
)
echo Build completed successfully!
pause
goto end

:run
echo Running the application...
if not exist "target\p2p-whiteboard.jar" (
    echo JAR file not found. Building project first...
    call mvn clean package
    if %ERRORLEVEL% NEQ 0 (
        echo Build failed!
        pause
        exit /b 1
    )
)
echo Starting P2P Whiteboard...
echo.
java -jar target\p2p-whiteboard.jar
goto end

:clean
echo Cleaning build artifacts...
call mvn clean
echo Clean completed successfully!
pause
goto end

:test
echo Running tests...
call mvn test
pause
goto end

:package
echo Packaging the application...
call mvn package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo Packaging failed!
    pause
    exit /b 1
)
echo Packaging completed successfully!
echo JAR file created: target\p2p-whiteboard.jar
pause
goto end

:help
echo.
echo Usage: build.bat [ACTION]
echo.
echo Actions:
echo   build      Build the project (default)
echo   run        Build and run the application
echo   clean      Clean build artifacts
echo   test       Run unit tests
echo   package    Package the application
echo   help       Show this help message
echo.
echo Examples:
echo   build.bat            ^(builds the project^)
echo   build.bat run        ^(builds and runs^)
echo   build.bat clean      ^(cleans build^)
echo.
pause
goto end

:end
endlocal
