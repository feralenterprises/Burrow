@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>nul || (echo [FAIL] Java 17+ required. & pause & exit /b 1)
if not exist "build\TrashPanda-UI-SHELL-R10C.jar" (echo [FAIL] Extract entire ZIP. JAR missing. & pause & exit /b 1)
echo Checking page lead-image PNG first, then exact filename fallback from OSRS Wiki API as UNAPPROVED review candidates.
echo Nothing is installed in TPT. Existing art is not overwritten.
java -cp "build\TrashPanda-UI-SHELL-R10C.jar" gg.feral.tpt.shell.WikiBossArtFetcher --fetch-all
if errorlevel 1 (echo [FAIL] Import was not completed. & pause & exit /b 1)
echo To inspect any candidates, run 04-REVIEW-BOSS-ART-WINDOWS.cmd
pause
endlocal
