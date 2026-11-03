@echo off
setlocal EnableExtensions
cd /d "%~dp0"
if errorlevel 1 goto failed

echo.
echo ===============================================================
echo   TrashPanda Tracker - BUILD 20.1 Windows development client
echo   RELEASE HOLD - not a normal RuneLite plugin installation
echo ===============================================================
echo.
call "%~dp0WINDOWS-BUILD20-PREFLIGHT.cmd"
if errorlevel 1 goto failed
if defined JAVA17_HOME set "JAVA_HOME=%JAVA17_HOME%"

echo.
echo [B20] Running genuine JDK 17 Gradle clean/test/jar first.
echo [B20] If the tests pass, the RuneLite developer client will start.
echo [B20] First launch may download Gradle and Maven/RuneLite dependencies.
echo.
call "%~dp0gradlew.bat" --no-daemon --console=plain clean test jar
if errorlevel 1 goto failed

echo.
echo [B20] Gradle BUILD + TEST passed on this PC. Starting RuneLite...
call "%~dp0gradlew.bat" --no-daemon --console=plain run
if errorlevel 1 goto failed

echo.
echo [B20] Developer client exited normally. Release remains on HOLD.
pause
exit /b 0

:failed
echo.
echo [B20] BUILD OR LAUNCH FAILED. Copy the FIRST error shown above.
echo [B20] No acceptance or gameplay test is claimed.
pause
exit /b 1
