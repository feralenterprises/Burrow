@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>nul || (echo Java 17 or newer is needed to open this standalone UI shell. & pause & exit /b 1)
if not exist "build\TrashPanda-UI-SHELL-R10C.jar" (echo Shell JAR missing from build folder. Please extract the full ZIP. & pause & exit /b 1)
java -jar "build\TrashPanda-UI-SHELL-R10C.jar"
if errorlevel 1 (echo UI shell did not open. The current plugin was NOT modified. & pause)
endlocal
