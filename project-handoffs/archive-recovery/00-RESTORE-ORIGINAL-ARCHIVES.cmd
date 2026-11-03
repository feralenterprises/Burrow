@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0RESTORE-ORIGINAL-ARCHIVES.ps1"
if errorlevel 1 (
  echo [FAIL] Restore blocked or checksum mismatch.
  pause
  exit /b 1
)
echo [PASS] Both original master archives restored.
pause
