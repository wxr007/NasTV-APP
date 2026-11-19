@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"

REM 读取 client-androidersion.properties 的 versionName，打 v* 标签并推送
REM 推送标签会触发 Actions：构建 APK 并自动创建 GitHub Release

set "TAG="
for /f "tokens=2 delims==" %%a in ('findstr /b "versionName=" client-androidersion.properties') do set "TAG=v%%a"
if not defined TAG goto :err_version

echo 目标版本: %TAG%

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
echo 已推送标签 %TAG%，Actions 会自动构建并创建 Release：
echo   https://github.com/wxr007/NasTV-APP/releases/tag/%TAG%
echo   https://github.com/wxr007/NasTV-APP/actions
exit /b 0

:err_version
echo [错误] 未从 client-androidersion.properties 读到 versionName
exit /b 1

:err_dirty
echo [错误] 工作区有未提交的改动，请先 commit：
git status --short
exit /b 1

:err_local_tag
echo [错误] 本地已存在标签 %TAG%，如需重发请先删除该标签
exit /b 1

:err_remote_tag
echo [错误] 远程已存在标签 %TAG%，请先提升 versionName 再发布
exit /b 1

:err_tag
echo [错误] 创建标签失败
exit /b 1

:err_push
echo [错误] 推送标签失败
exit /b 1
