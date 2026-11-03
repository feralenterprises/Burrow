@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp000-PREPARE-QA-SOURCES.ps1"
if errorlevel 1 (
 echo [FAIL] QA source extraction or SHA verification failed.
 pause
 exit /b 1
)
echo [PASS] QA source staging; not yet an integrated native build.
pause
