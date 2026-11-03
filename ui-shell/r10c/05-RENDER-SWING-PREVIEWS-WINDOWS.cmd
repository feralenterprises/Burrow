@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>nul || (echo [FAIL] Java 17+ is required. & pause & exit /b 1)
java -jar "build\TrashPanda-UI-SHELL-R10C.jar" --screenshots "previews"
if errorlevel 1 (echo [FAIL] Could not render previews. & pause & exit /b 1)
echo [PASS] Standalone Swing screenshots saved in previews. No game account connection.
pause
endlocal
