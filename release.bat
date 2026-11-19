@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
cd /d "%~dp0"

REM 读取 client-android\version.properties 的 versionName，打 v* 标签并推送
REM 推送标签会触发 Actions：构建 APK -> 自动创建 Release 并上传产物

for /f "tokens=2 delims==" %%a in ('findstr /b "versionName=" client-android\version.properties') do set "NAME=%%a"
if not defined NAME (
  echo [错误] 未从 client-android\version.properties 读到 versionName
  exit /b 1
)
set "TAG=v%NAME%"
echo 目标版本：%TAG%
echo.

for /f "delims=" %%i in ('git status --porcelain') do (
  echo [错误] 工作区有未提交的改动，请先 commit 再发布：
  git status --short
  exit /b 1
)

git rev-parse --verify "refs/tags/%TAG%" >nul 2>&1
if not errorlevel 1 (
  echo [错误] 本地已存在标签 %TAG%
  echo        如需重发：git tag -d %TAG%  ^&^&  git push origin :refs/tags/%TAG%
  exit /b 1
)

git ls-remote --exit-code --tags origin "refs/tags/%TAG%" >nul 2>&1
if not errorlevel 1 (
  echo [错误] 远程已存在标签 %TAG%，请先提升 versionName 再发布
  exit /b 1
)

git tag -a "%TAG%" -m "NasTV %TAG%"
if errorlevel 1 exit /b 1

git push origin "%TAG%"
if errorlevel 1 exit /b 1

echo.
echo 已推送标签 %TAG%，Actions 将自动构建并创建 Release：
echo   https://github.com/wxr007/NasTV-APP/releases/tag/%TAG%
echo   https://github.com/wxr007/NasTV-APP/actions
endlocal
