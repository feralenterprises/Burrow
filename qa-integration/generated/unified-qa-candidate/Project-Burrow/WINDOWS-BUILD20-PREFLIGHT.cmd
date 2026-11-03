@echo off
setlocal EnableExtensions
cd /d "%~dp0"
if errorlevel 1 goto failed

echo [B20] Checking Windows Java 17 and complete Gradle wrapper...
if not exist "build.gradle" goto missingProject
if not exist "gradlew.bat" goto missingWrapper
if not exist "gradle\wrapper\gradle-wrapper.jar" goto missingWrapper
if not exist "gradle\wrapper\gradle-wrapper.properties" goto missingWrapper
if not exist "src\test\java\gg\feral\burrow\runelite\BurrowPluginTest.java" goto missingProject

rem JAVA17_HOME takes precedence for project runs; never point at the bin folder.
if defined JAVA17_HOME set "JAVA_HOME=%JAVA17_HOME%"
if defined JAVA_HOME goto javaFromHome

where java.exe >nul 2>&1
if errorlevel 1 goto noJava
where javac.exe >nul 2>&1
if errorlevel 1 goto noJava
set "JAVA_BIN=java.exe"
set "JAVAC_BIN=javac.exe"
goto verifyVersion

:javaFromHome
set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
set "JAVAC_BIN=%JAVA_HOME%\bin\javac.exe"
if not exist "%JAVA_BIN%" goto noJava
if not exist "%JAVAC_BIN%" goto noJava

:verifyVersion
"%JAVA_BIN%" -XshowSettings:properties -version 2>&1 | findstr /C:"java.specification.version = 17" >nul
if errorlevel 1 goto wrongJava
"%JAVAC_BIN%" -version 2>&1 | findstr /R /C:"javac 17[. ]" >nul
if errorlevel 1 goto wrongJava

echo [PASS] Java 17 runtime and compiler.
echo [PASS] Gradle wrapper and RuneLite development entrypoint present.
echo [NOTE] This does not run Gradle or confirm native RuneLite behavior.
exit /b 0

:missingProject
echo [FAIL] Source project or developer-client entrypoint missing. Extract the FULL ZIP.
goto failed
:missingWrapper
echo [FAIL] Gradle wrapper is incomplete. Extract the FULL ZIP.
goto failed
:noJava
echo [FAIL] JDK 17 runtime/compiler not found.
echo Set JAVA17_HOME or JAVA_HOME to your JDK 17 install directory, NOT its bin folder.
goto failed
:wrongJava
echo [FAIL] Selected Java runtime/compiler is not JDK 17.
"%JAVA_BIN%" -version 2>&1
"%JAVAC_BIN%" -version 2>&1
echo Set JAVA17_HOME to a JDK 17 install, or set IntelliJ Gradle JVM to JDK 17.
goto failed
:failed
exit /b 1
