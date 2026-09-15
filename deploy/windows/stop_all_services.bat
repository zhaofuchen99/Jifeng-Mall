@echo off
REM =====================================================================
REM Stop ALL 13 backend services (fat-jar OR IDEA-launched).
REM
REM HOW IT FINDS THEM: by the jar/module path in the process command line
REM   -- `jifeng-mall-<x>-api\target` or `jifeng-mall-gateway-webflux\target`.
REM   That matches both launch styles (IDEA uses -classpath ...\target\classes,
REM   fat jar uses -jar ...\target\xxx.jar) and cannot match Nacos
REM   (D:\nacos\target\nacos-server.jar) or IDEA itself.
REM
REM WHY ONE POWERSHELL CALL, NOT ONE PER SERVICE:
REM   each Get-CimInstance Win32_Process query costs ~15s on a loaded box;
REM   13 of them blew a 2-minute timeout and only half the services stopped.
REM   A single query that filters on a regex is a couple of seconds.
REM
REM This file is intentionally PURE ASCII (cmd parses .bat with the ANSI
REM code page; UTF-8 Chinese comments become bogus commands).
REM =====================================================================
setlocal

echo Stopping all jifeng-mall services...
powershell -NoProfile -Command "$p = Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { $_.CommandLine -match '(jifeng-mall-[a-z-]+-api|jifeng-mall-gateway-webflux)[\\\\/]target' }; if ($p) { $p | ForEach-Object { Write-Host ('  stopping PID ' + $_.ProcessId) ; Stop-Process -Id $_.ProcessId -Force } } else { Write-Host '  (none running)' }"

echo.
echo Done. Ports 8888 and 10010-10021 should be free now.
echo Check: netstat -ano ^| findstr LISTENING ^| findstr /R ":8888 :1001 :1002"
endlocal
exit /b 0
