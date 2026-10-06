@echo off
setlocal
set "PWSH_COMMAND=pwsh.exe"
where.exe pwsh.exe >nul 2>&1
if errorlevel 1 (
    if exist "%ProgramFiles%\PowerShell\7\pwsh.exe" set "PWSH_COMMAND=%ProgramFiles%\PowerShell\7\pwsh.exe"
    if exist "%USERPROFILE%\.cache\codex-runtimes\codex-primary-runtime\dependencies\native\powershell\pwsh.exe" set "PWSH_COMMAND=%USERPROFILE%\.cache\codex-runtimes\codex-primary-runtime\dependencies\native\powershell\pwsh.exe"
)
"%PWSH_COMMAND%" -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\independent\start.ps1" %*
set "launcherExitCode=%errorlevel%"
if not "%launcherExitCode%"=="0" pause
endlocal & exit /b %launcherExitCode%
