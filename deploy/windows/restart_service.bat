@echo off
REM =====================================================================
REM Restart ONE backend service from its fat jar.
REM
REM USAGE:
REM   restart_service.bat <module-dir-name> [log-name]
REM
REM   e.g.  restart_service.bat jifeng-mall-order-api
REM         restart_service.bat jifeng-mall-order-api order-api
REM
REM The module name must match the directory under jifeng-mall-api\, because
REM that is also what appears in the running process command line -- the stop
REM step finds the process by matching it.
REM
REM PREREQUISITES:
REM   1) the fat jar must already exist:
REM        mvnw.cmd -o -DskipTests -pl :<module> install
REM      (stop the service BEFORE installing, otherwise Maven's repackage
REM       step fails with "Unable to rename ... .jar.original")
REM   2) service must be started this way from now on -- IDEA-launched
REM      instances are the ones this script stops, but IDEA will show it
REM      as stopped afterwards.
REM
REM WHY "cmd /c" INSIDE start:
REM   "start name java ... > log" redirects start's OWN output, not the
REM   child's, so the log file stays empty forever. The redirect has to live
REM   inside the child command line. And an inner quoted path would break
REM   cmd /c quoting, hence the PROGRA~1 8.3 short name for Program Files.
REM
REM This file is intentionally PURE ASCII: cmd parses .bat with the ANSI
REM code page, so UTF-8 Chinese comments get split into bogus commands.
REM =====================================================================
setlocal

set "MODULE=%~1"
set "LOGNAME=%~2"
if "%MODULE%"=="" (
    echo [ERROR] usage: restart_service.bat ^<module-dir-name^> [log-name]
    exit /b 1
)
if "%LOGNAME%"=="" set "LOGNAME=%MODULE%"

set "JARROOT=D:\IDEA_projects\shoplook2026-parent\jifeng-mall-api"
set "LOGDIR=D:\run-logs"
set "JAVAEXE=C:\PROGRA~1\Java\jdk-25.0.4\bin\java.exe"
set "JAR=%JARROOT%\%MODULE%\target\%MODULE%-1.0.0.jar"

if not exist "%JAR%" (
    echo [ERROR] jar not found: %JAR%
    echo         build it first:  mvnw.cmd -o -DskipTests -pl :%MODULE% install
    exit /b 2
)
if not exist "%LOGDIR%" mkdir "%LOGDIR%"

echo Stopping %MODULE% (if running)...
powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { $_.CommandLine -like '*%MODULE%*' } | ForEach-Object { Write-Host ('  killing PID ' + $_.ProcessId); Stop-Process -Id $_.ProcessId -Force }"

echo Waiting for the port to be released...
timeout /t 4 /nobreak >nul

echo Starting %MODULE% ...
start "%LOGNAME%" /min cmd /c "%JAVAEXE% -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar %JAR% > %LOGDIR%\%LOGNAME%.log 2>&1"
echo   started. log: %LOGDIR%\%LOGNAME%.log

endlocal
exit /b 0
