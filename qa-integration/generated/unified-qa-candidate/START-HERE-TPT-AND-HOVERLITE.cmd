@echo off
setlocal EnableExtensions
call "%~dp000-START-BOTH-NO-REPEAT-QA.cmd"
exit /b %errorlevel%
