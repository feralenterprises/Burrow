@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>nul || (echo [FAIL] Java 17+ is required. & pause & exit /b 1)
if not exist "build\TrashPanda-UI-SHELL-R10C.jar" (echo [FAIL] Missing shell JAR. Extract the whole ZIP. & pause & exit /b 1)
java -cp "build\TrashPanda-UI-SHELL-R10C.jar" gg.feral.tpt.shell.BossArtReviewer
if errorlevel 1 echo [FAIL] Art review UI did not open. Nothing in TPT was modified.
pause
endlocal
