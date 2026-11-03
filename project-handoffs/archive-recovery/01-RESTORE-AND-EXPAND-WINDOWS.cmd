@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0EXPAND-VERIFIED-MASTER-ARCHIVES.ps1"
if errorlevel 1 (
  echo [FAIL] Archive restore, expansion, or file-by-file verification failed.
  pause
  exit /b 1
)
echo [PASS] Full master archive file trees restored locally.
pause
