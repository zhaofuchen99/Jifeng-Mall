@echo off
REM =====================================================================
REM Run a .sql file against the jifeng database. Used by the verification
REM scripts that need to touch the DB directly (e.g. backdating an order's
REM created_time to simulate a timeout).
REM
REM USAGE:  run_sql.bat <path-to-sql-file>
REM
REM WHY A WRAPPER INSTEAD OF PASSING THE STATEMENT DIRECTLY:
REM   Python's subprocess re-quotes an argv list, and cmd.exe strips the
REM   outer quotes of its /c argument -- so `cmd /c "C:\Program Files\...\mysql.exe" ...`
REM   reaches cmd as a bare `C:\Program` and dies with "not recognized as a
REM   command". The same class of bug silently broke the Redis cleanup once.
REM   Taking a single space-free path argument sidesteps all of it.
REM
REM This file is intentionally PURE ASCII (cmd parses .bat with the ANSI
REM code page; UTF-8 Chinese comments become bogus commands).
REM =====================================================================
setlocal

set "MYSQL=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
set "DBNAME=shoplook2026"
set "DBUSER=root"
set "DBPASS=09010402"

if "%~1"=="" (
    echo [ERROR] usage: run_sql.bat ^<path-to-sql-file^>
    exit /b 1
)
if not exist "%~1" (
    echo [ERROR] sql file not found: %~1
    exit /b 1
)

"%MYSQL%" --default-character-set=utf8mb4 -u%DBUSER% -p%DBPASS% %DBNAME% < "%~1"
exit /b %errorlevel%
