@echo off
chcp 65001 >nul
cd /d "%~dp0client-android"

set APK_DIR=app\build\outputs\apk\release

REM 查找现有的 APK
set "APK_FILE="
for %%f in (%APK_DIR%\*.apk) do (
    set "APK_FILE=%%f"
    goto :install
)

REM 没找到，先编译
echo APK not found, building first...
call .\gradlew.bat assembleRelease
if %errorlevel% neq 0 (
    echo Build failed.
    pause
    exit /b 1
)

for %%f in (%APK_DIR%\*.apk) do (
    set "APK_FILE=%%f"
    goto :install
)

:install
if "%APK_FILE%"=="" (
    echo No APK found after build.
    pause
    exit /b 1
)
echo Installing %APK_FILE% to device...
adb -d install -r "%APK_FILE%"
if %errorlevel% equ 0 (
    echo Install success!
) else (
    echo Install failed. Please check device connection.
)
