@echo off
REM =====================================================================
REM Start ALL 13 backend services as fat jars, without IntelliJ IDEA.
REM
REM WHY THIS EXISTS: the services used to be launched from IDEA Run
REM   configurations, so closing IDEA stopped everything and a machine
REM   reboot meant clicking 13 entries by hand. Running them from fat jars
REM   makes this a real deployment: IDEA can be closed, and one command
REM   brings the whole system up.
REM
REM ORDER: reference data first (rbac/user/member/region/...), business
REM   services next, GATEWAY LAST -- it routes to the others, so starting it
REM   last avoids a burst of 503s during startup.
REM
REM LOGS: D:\run-logs\<name>.log. The redirect must live INSIDE the child
REM   command line: `start "x" java ... > log` redirects start's own output
REM   and leaves the log file empty forever. And the inner command is
REM   quoted, so Program Files is referenced by its 8.3 short name
REM   (PROGRA~1) to avoid cmd /c quote stripping.
REM
REM BUILD FIRST (nothing may be holding the jars):
REM   cd /d <repo>  &&  set JAVA_HOME=C:\Program Files\Java\jdk-25.0.4
REM   mvnw.cmd -DskipTests package
REM
REM This file is intentionally PURE ASCII: cmd parses .bat with the ANSI
REM code page, so UTF-8 Chinese comments get split into bogus commands.
REM A 5s pause between launches keeps 13 JVMs from initialising at once.
REM =====================================================================
setlocal

set "ROOT=D:\IDEA_projects\shoplook2026-parent"
set "JARROOT=%ROOT%\jifeng-mall-api"
set "LOGDIR=D:\run-logs"
set "JAVAEXE=C:\PROGRA~1\Java\jdk-25.0.4\bin\java.exe"

if not exist "%LOGDIR%" mkdir "%LOGDIR%"

echo Starting 13 services from fat jars...
echo.

call :launch jifeng-mall-rbac-api rbac-api
call :launch jifeng-mall-user-api user-api
call :launch jifeng-mall-member-api member-api
call :launch jifeng-mall-region-api region-api
call :launch jifeng-mall-member-address-api member-address-api
call :launch jifeng-mall-brand-api brand-api
call :launch jifeng-mall-category-api category-api
call :launch jifeng-mall-good-api good-api
call :launch jifeng-mall-cart-api cart-api
call :launch jifeng-mall-order-api order-api
call :launch jifeng-mall-upload-api upload-api
call :launch jifeng-mall-seckill-api seckill-api
call :launch jifeng-mall-gateway-webflux gateway

echo.
echo ============================================================
echo  All launched. Give them ~40s, then verify:
echo    netstat -ano ^| findstr LISTENING ^| findstr /R ":8888 :1001 :1002"
echo  Logs: %LOGDIR%
echo ============================================================
endlocal
exit /b 0

:launch
REM Resolve the jar path: the 12 api modules live under jifeng-mall-api\,
REM the gateway sits at the repo root -- hence the two candidates.
REM No delayed expansion here on purpose: this is a plain subroutine, so
REM %JAR% expands correctly line by line.
set "JAR="
if exist "%JARROOT%\%1\target\%1-1.0.0.jar" set "JAR=%JARROOT%\%1\target\%1-1.0.0.jar"
if not defined JAR if exist "%ROOT%\%1\target\%1-1.0.0.jar" set "JAR=%ROOT%\%1\target\%1-1.0.0.jar"
if not defined JAR (
    echo   [SKIP] %1  -- jar not found. Build first with: mvnw.cmd -DskipTests package
    exit /b 0
)
echo   starting %2
start "%2" /min cmd /c "%JAVAEXE% -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar %JAR% > %LOGDIR%\%2.log 2>&1"
timeout /t 5 /nobreak >nul
exit /b 0
