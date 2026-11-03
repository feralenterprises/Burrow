@echo off
setlocal EnableExtensions
cd /d "%~dp0Project-Burrow" || goto failed
 echo ============================================================
echo TPT W07.5.1 + HOVERLITE - FULL GRADLE TEST / JDK 17
echo RuneLite 1.13.5 - Owner verification, not required for launch
echo ============================================================
call "WINDOWS-BUILD20-PREFLIGHT.cmd"
if errorlevel 1 goto failed
if defined JAVA17_HOME set "JAVA_HOME=%JAVA17_HOME%"
call "gradlew.bat" --no-daemon --console=plain --refresh-dependencies -PruneLiteVersion=1.13.5 clean test hoverliteDevClasses jar
if errorlevel 1 goto failed
echo [PASS] Windows Gradle test completed.
pause
exit /b 0
:failed
echo [FAIL] Copy the FIRST error above.
pause
exit /b 1
