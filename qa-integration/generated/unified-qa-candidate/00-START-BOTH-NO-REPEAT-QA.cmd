@echo off
setlocal EnableExtensions
set "ROOT=%~dp0"
if exist "%ROOT%Project-Burrow\gradlew.bat" (
  set "PROJECT_DIR=%ROOT%Project-Burrow"
) else if exist "%ROOT%gradlew.bat" (
  set "PROJECT_DIR=%ROOT%"
) else (
  echo [FAIL] Project-Burrow\gradlew.bat not found. Extract the complete ZIP first.
  goto failed
)
cd /d "%PROJECT_DIR%" || goto failed

title TrashPanda Tracker + HoverLite - RuneLite 1.13.5

echo ============================================================
echo TPT W07.5.1 + HOVERLITE v0.2.8 - ONE RUNELITE CLIENT
echo RuneLite 1.13.5 - OUTDATED CLIENT REPAIR
echo No test-suite repeat and no gameplay source changes
echo ============================================================
call "WINDOWS-BUILD20-PREFLIGHT.cmd"
if errorlevel 1 goto failed
if defined JAVA17_HOME set "JAVA_HOME=%JAVA17_HOME%"
echo [START] Updating RuneLite dependency and launching both plugins...
call "gradlew.bat" --no-daemon --console=plain --refresh-dependencies -PruneLiteVersion=1.13.5 runBoth
if errorlevel 1 goto failed
echo [INFO] RuneLite closed normally.
pause
exit /b 0
:failed
echo.
echo [FAIL] RuneLite was not successfully launched.
echo If this is error_game_js5connect_outofdate, first check whether
 echo normal installed RuneLite launches. Inspect %%USERPROFILE%%\.runelite\logs\client.log.
echo Please send the FIRST error above or the end of client.log (redact account details).
pause
exit /b 1
