@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>nul || (echo [FAIL] Java 17+ is required. & pause & exit /b 1)
if not exist "build\TrashPanda-UI-SHELL-R10C.jar" (echo [FAIL] Missing shell JAR. Extract the whole ZIP. & pause & exit /b 1)
java -jar "build\TrashPanda-UI-SHELL-R10C.jar" --self-test
if errorlevel 1 (echo [FAIL] Main shell regression failed. & pause & exit /b 1)
java -cp "build\TrashPanda-UI-SHELL-R10C.jar" gg.feral.tpt.shell.BossArtReviewer --self-test
if errorlevel 1 (echo [FAIL] Art importer regression failed. & pause & exit /b 1)
java -cp "build\TrashPanda-UI-SHELL-R10C.jar" gg.feral.tpt.shell.WikiBossArtFetcher --self-test
if errorlevel 1 (echo [FAIL] Wiki importer regression failed. & pause & exit /b 1)
echo [PASS] Offline UI shell, art review and Wiki importer validation. Native RuneLite QA NOT performed.
pause
endlocal
