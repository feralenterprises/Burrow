@echo off
setlocal EnableExtensions
cd /d "%~dp0Project-Burrow" || goto failed
echo =============================================================
echo FERAL DUAL DEV - ONE RUNELITE CLIENT / TWO LOCAL PLUGINS
echo TrashPanda W07.5.1 + HoverLite v0.2.8 OWNER QA
echo RuneLite 1.13.5 - OWNER WINDOWS QA / RELEASE HOLD
echo =============================================================
call "WINDOWS-BUILD20-PREFLIGHT.cmd"
if errorlevel 1 goto failed
if defined JAVA17_HOME set "JAVA_HOME=%JAVA17_HOME%"
echo [1/2] Run tests and compile both plugins...
call "gradlew.bat" --no-daemon --console=plain -PruneLiteVersion=1.13.5 clean test hoverliteDevClasses
if errorlevel 1 goto failed
echo [2/2] Starting ONE RuneLite developer client...
call "gradlew.bat" --no-daemon --console=plain -PruneLiteVersion=1.13.5 runBoth
if errorlevel 1 goto failed
echo [INFO] RuneLite developer client closed. RELEASE HOLD.
pause
exit /b 0
:failed
echo [FAIL] Copy the FIRST relevant error above.
pause
exit /b 1
