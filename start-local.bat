@echo off
setlocal

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-local.ps1" %*
set "launcherExitCode=%errorlevel%"
if not "%launcherExitCode%"=="0" (
    echo.
    echo Local startup failed. Review the message above, then try again.
    pause
)

endlocal & exit /b %launcherExitCode%
