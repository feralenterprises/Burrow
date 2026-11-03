@echo off
setlocal
cd /d "%~dp0"
if not exist "%~dp0generated\unified-qa-candidate\Project-Burrow\gradlew.bat" (
  echo [FAIL] Missing unified QA candidate. Run 00-PREPARE-QA-SOURCES.cmd first, then ensure unified-qa-candidate exists.
  pause
  exit /b 1
)
cd /d "%~dp0generated\unified-qa-candidate\Project-Burrow"
echo ============================================================
echo TrashPanda Tracker UNIFIED QA candidate - JDK17 Gradle tests
echo RELEASE HOLD - not Plugin Hub / not main / not native proof
echo ============================================================
java -version
call gradlew.bat --no-daemon test
set TEST_EXIT=%ERRORLEVEL%
echo.
echo See qa-integration\QA-RESULTS.md — expect 674/681 offline PASS with 7 known W09 Pathfinder/Sidebar failures until contracts are reconciled.
if not "%TEST_EXIT%"=="0" (
  echo [FAIL] Gradle reported test failures. Compile may still be green; live RuneLite QA still required.
  pause
  exit /b %TEST_EXIT%
)
echo [PASS] All offline Gradle tests green. Live RuneLite owner QA still required.
pause
exit /b 0
