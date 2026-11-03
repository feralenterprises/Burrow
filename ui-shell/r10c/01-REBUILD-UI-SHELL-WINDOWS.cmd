@echo off
setlocal
cd /d "%~dp0"
where javac >nul 2>nul || (echo Java JDK 17+ with javac is needed to rebuild. & pause & exit /b 1)
if not exist build\classes mkdir build\classes
javac --release 17 -encoding UTF-8 -d build\classes src\gg\feral\tpt\shell\*.java
if errorlevel 1 (echo COMPILE FAILED & pause & exit /b 1)
xcopy /E /I /Y assets build\classes\assets >nul
jar --create --file build\TrashPanda-UI-SHELL-R10C.jar --main-class gg.feral.tpt.shell.TrashPandaShell -C build\classes .
if errorlevel 1 (echo PACKAGING FAILED & pause & exit /b 1)
call "00-OPEN-UI-SHELL-WINDOWS.cmd"
endlocal
