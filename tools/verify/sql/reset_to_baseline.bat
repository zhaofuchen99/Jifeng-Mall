@echo off
REM =====================================================================
REM Reset test data to the seed baseline (MySQL + Redis).
REM
REM WHY THIS IS A .BAT AND NOT JUST A .SQL:
REM   Part of the seckill state lives only in Redis (seckill:stock,
REM   seckill:user, seckill:order). SQL cannot reach it. If you only
REM   reset the DB, the per-user purchase marks left by the previous
REM   benchmark are still there -- and they have NO TTL -- so the next
REM   benchmark fails with 7003 "already participated". That looks like
REM   "stock was reset but nobody can buy", which is very hard to debug.
REM   This is exactly why the benchmark used to be non-reproducible.
REM
REM USAGE (run on the Windows side, cmd or double-click):
REM   cd /d <directory of this file>
REM   reset_to_baseline.bat
REM
REM NOTE: this file is intentionally PURE ASCII. cmd parses a .bat with the
REM   ANSI code page, so UTF-8 Chinese comments get split into bogus
REM   commands. Keep it ASCII; the Chinese explanation lives in
REM   reset_to_baseline.sql (which mysql reads as utf8mb4) and in
REM   tools/verify/README.md.
REM
REM Adjust these three paths for your own environment.
REM =====================================================================
setlocal

set "MYSQL=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
set "REDISCLI=D:\Redis\redis-windows-8.8.0\redis-cli.exe"
set "DBNAME=shoplook2026"
set "DBUSER=root"
set "DBPASS=09010402"

set "SQLFILE=%~dp0reset_to_baseline.sql"

if not exist "%SQLFILE%" (
    echo [ERROR] cannot find %SQLFILE%
    exit /b 1
)

echo ============================================================
echo  [1/2] Reset MySQL (orders / accounts / stock / RBAC)
echo ============================================================
"%MYSQL%" --default-character-set=utf8mb4 -u%DBUSER% -p%DBPASS% %DBNAME% < "%SQLFILE%"
if errorlevel 1 (
    echo [ERROR] MySQL reset failed
    exit /b 1
)

echo.
echo ============================================================
echo  [2/2] Clear Redis seckill keys
echo ============================================================
if not exist "%REDISCLI%" (
    echo [WARN] redis-cli not found: %REDISCLI%
    echo        Skipped Redis cleanup. Before benchmarking run manually:
    echo          redis-cli SET seckill:stock:1 50
    echo          redis-cli --scan --pattern "seckill:user:*"   then DEL each
    echo          redis-cli --scan --pattern "seckill:order:*"  then DEL each
    exit /b 0
)

"%REDISCLI%" PING >nul 2>&1
if errorlevel 1 (
    echo [WARN] Redis not reachable, skipped cleanup
    exit /b 0
)

REM Delete by pattern SERVER-SIDE via EVAL, in a single direct invocation.
REM
REM Do NOT rewrite this as `for /f %%k in ('"%REDISCLI%" --scan ...')`:
REM cmd strips the outer quotes of the command string inside for /f, so the
REM exe path gets mangled and the loop dies with "system cannot find the
REM path" -- while still reporting success. That bug shipped once here and
REM silently left seckill:user:* / seckill:order:* behind.
REM
REM `keys` is O(N); fine for a demo DB. Use SCAN in a real production job.
"%REDISCLI%" EVAL "local n=0 for _,p in ipairs({'seckill:user:*','seckill:order:*'}) do for _,k in ipairs(redis.call('keys',p)) do redis.call('del',k) n=n+1 end end return n" 0 > "%TEMP%\jifeng_redis_del.txt"
set /p DELETED=<"%TEMP%\jifeng_redis_del.txt"

REM Write the stock key back to the baseline 50. Safe while the activity
REM is running: the preheat task uses SET IF ABSENT and will not overwrite.
"%REDISCLI%" SET seckill:stock:1 50 >nul

"%REDISCLI%" EVAL "return #redis.call('keys','seckill:user:*')" 0 > "%TEMP%\jifeng_redis_left.txt"
set /p LEFT=<"%TEMP%\jifeng_redis_left.txt"

"%REDISCLI%" GET seckill:stock:1 > "%TEMP%\jifeng_redis_stock.txt"
set /p STOCK=<"%TEMP%\jifeng_redis_stock.txt"

echo   deleted seckill:user / seckill:order keys : %DELETED%
echo   seckill:stock:1                            : %STOCK%
echo   remaining seckill:user / seckill:order keys: %LEFT%

REM Verify instead of assuming. A cleanup script that reports success while
REM leaving keys behind is worse than no cleanup script at all.
if not "%LEFT%"=="0" (
    echo.
    echo [ERROR] Redis still has %LEFT% leftover seckill:user / seckill:order keys.
    echo         The next benchmark WILL fail with 7003. Investigate before running it.
    exit /b 2
)
if not "%STOCK%"=="50" (
    echo.
    echo [ERROR] seckill:stock:1 is "%STOCK%", expected 50.
    exit /b 3
)

echo.
echo ============================================================
echo  Done. The environment is back at baseline.
echo ============================================================
endlocal
exit /b 0
