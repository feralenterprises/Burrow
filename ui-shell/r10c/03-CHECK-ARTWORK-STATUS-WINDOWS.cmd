@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>nul || (echo Java 17+ required. & pause & exit /b 1)
java -jar "build\TrashPanda-UI-SHELL-R10C.jar" --audit-art
echo.
echo FALLBACK means the genuine OSRS mini sprite remains; no high-res art has been approved.
echo The reported recorded approval is NOT independent license verification.
pause
endlocal
