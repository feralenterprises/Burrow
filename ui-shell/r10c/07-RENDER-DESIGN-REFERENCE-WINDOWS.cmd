@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>nul || (echo Java 17+ is required. & pause & exit /b 1)
java -jar "build\TrashPanda-UI-SHELL-R10C.jar" --screenshots-reference "previews-reference"
if errorlevel 1 (echo [FAIL] Could not render design reference captures. & pause & exit /b 1)
echo [PASS] Saved standalone Swing design comparison images; not certified OSRS artwork.
pause
endlocal
