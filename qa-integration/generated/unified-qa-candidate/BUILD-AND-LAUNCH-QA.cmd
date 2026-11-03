@echo off
setlocal
cd /d "%~dp0Project-Burrow"
echo ============================================================
echo TrashPanda Tracker UNIFIED QA - build tests then launch dual-dev
echo Requires Windows JDK 17. RELEASE HOLD. No Plugin Hub publish.
echo ============================================================
java -version
call gradlew.bat --no-daemon test runBoth
if errorlevel 1 ( echo [FAIL] Build, test, or RuneLite launch failed. & pause & exit /b 1 )
echo [PASS] Launcher exited. Confirm Bank O / Bank Memory / account hop in live client.
pause
exit /b 0
