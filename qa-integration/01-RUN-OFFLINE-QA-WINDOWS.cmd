@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp000-PREPARE-QA-SOURCES.ps1"
if errorlevel 1 goto failed
if not exist "%~dp0generated\pathfinder-w09\Project-Burrow\gradlew.bat" goto failed
cd /d "%~dp0generated\pathfinder-w09\Project-Burrow"
echo ===================================================
echo Pathfinder W09 isolated candidate: Java 17 offline QA
echo Not a combined TPT release or native RuneLite proof
echo ===================================================
java -version
call gradlew.bat --no-daemon test
if errorlevel 1 goto failed
echo [PASS] Isolated W09 Gradle tests; live RuneLite QA still required.
pause
exit /b 0
:failed
echo [FAIL] Offline candidate build or source preflight failed.
pause
exit /b 1
