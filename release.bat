@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"

REM Read versionName from client-android/version.properties,
REM then create tag v<versionName> and push it.
REM Pushing the tag triggers GitHub Actions to build APKs and publish a Release.

set "TAG="
for /f "tokens=2 delims==" %%a in ('findstr /b "versionName=" client-android\version.properties') do set "TAG=v%%a"
if not defined TAG goto :err_version

echo [INFO] target version: %TAG%

git diff-index --quiet HEAD --
if errorlevel 1 goto :err_dirty

git rev-parse --verify "refs/tags/%TAG%" >nul 2>&1
if not errorlevel 1 goto :err_local_tag

git ls-remote --exit-code --tags origin "refs/tags/%TAG%" >nul 2>&1
if not errorlevel 1 goto :err_remote_tag

git tag -a "%TAG%" -m "NasTV %TAG%"
if errorlevel 1 goto :err_tag

git push origin "%TAG%"
if errorlevel 1 goto :err_push

echo.
echo [OK] tag %TAG% pushed. Actions will build and publish the release:
echo      https://github.com/wxr007/NasTV-APP/actions
echo      https://github.com/wxr007/NasTV-APP/releases/tag/%TAG%
exit /b 0

:err_version
echo [ERROR] versionName not found in client-android/version.properties
exit /b 1

:err_dirty
echo [ERROR] working tree has uncommitted changes, commit first:
git status --short
exit /b 1

:err_local_tag
echo [ERROR] local tag %TAG% already exists
exit /b 1

:err_remote_tag
echo [ERROR] remote tag %TAG% already exists, bump versionName first
exit /b 1

:err_tag
echo [ERROR] failed to create tag
exit /b 1

:err_push
echo [ERROR] failed to push tag
exit /b 1
