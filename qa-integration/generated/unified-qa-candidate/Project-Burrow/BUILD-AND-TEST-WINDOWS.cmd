@echo off
setlocal EnableExtensions
cd /d "%~dp0"
if errorlevel 1 goto failed
echo.
echo TRASHPANDA TRACKER - BUILD 20.1 - Java 17 build and test only
echo.
call "%~dp0WINDOWS-BUILD20-PREFLIGHT.cmd"
if errorlevel 1 goto failed
if defined JAVA17_HOME set "JAVA_HOME=%JAVA17_HOME%"
call "%~dp0gradlew.bat" --no-daemon --console=plain clean test jar
if errorlevel 1 goto failed
echo.
echo [PASS] Build 20.1 compiled and tests passed on this PC.
echo This does not prove native RuneLite behavior or release acceptance.
pause
exit /b 0
:failed
echo.
echo [FAIL] Build 20.1 preflight or Gradle gate failed. Copy FIRST error.
pause
exit /b 1
