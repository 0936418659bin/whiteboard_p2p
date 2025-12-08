@echo off
REM Compile P2P Whiteboard without Maven
REM Batch script to compile all Java files

setlocal enabledelayedexpansion

set "sourceDir=%~dp0src\main\java"
set "targetDir=%~dp0target\classes"
set "libDir=%~dp0lib"

REM Create target directory if not exists
if not exist "%targetDir%" (
    mkdir "%targetDir%"
    echo Created %targetDir%
)

REM Check for javac
where javac >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Error: javac not found in PATH
    echo Please install JDK 17 or higher
    pause
    exit /b 1
)

echo Checking Java compiler...
javac -version
echo.

REM Build classpath with all JARs in lib directory
set "classpath=%libDir%"
for %%f in ("%libDir%\*.jar") do (
    set "classpath=!classpath!;%%f"
)

echo Starting compilation...
echo Source: %sourceDir%
echo Output: %targetDir%
echo.

REM Count Java files
setlocal enabledelayedexpansion
set "count=0"
for /r "%sourceDir%" %%f in (*.java) do set /a count+=1
echo Found %count% Java files to compile
echo.

REM Compile
javac -d "%targetDir%" -cp "%classpath%" -encoding UTF-8 "%sourceDir%\com\whiteboard\drawing\*.java" "%sourceDir%\com\whiteboard\network\*.java" "%sourceDir%\com\whiteboard\ui\*.java" "%sourceDir%\com\whiteboard\sync\*.java" "%sourceDir%\com\whiteboard\*.java"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Compilation successful!
    echo Output directory: %targetDir%
    
    REM Count compiled classes
    setlocal enabledelayedexpansion
    set "classCount=0"
    for /r "%targetDir%" %%f in (*.class) do set /a classCount+=1
    echo Generated %classCount% class files
    
    exit /b 0
) else (
    echo.
    echo Compilation failed!
    pause
    exit /b 1
)
