@echo off
setlocal
cd /d "%~dp0Project-Burrow"
echo ============================================================
echo TrashPanda Tracker UNIFIED QA candidate - JDK17 Gradle tests
echo RELEASE HOLD - not Plugin Hub / not main / not native proof
echo ============================================================
java -version
call gradlew.bat --no-daemon test
set TEST_EXIT=%ERRORLEVEL%
echo.
echo Expect 674/681 offline PASS with 7 known W09 Pathfinder/Sidebar failures until contracts are reconciled. See qa-integration\QA-RESULTS.md
if not "%TEST_EXIT%"=="0" (
  echo [FAIL] Gradle reported test failures. Live RuneLite QA still required.
  pause
  exit /b %TEST_EXIT%
)
echo [PASS] All offline Gradle tests green. Live RuneLite owner QA still required.
pause
exit /b 0
