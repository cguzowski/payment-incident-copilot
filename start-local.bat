@echo off
setlocal

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-local.ps1" %*
set "launcherExitCode=%errorlevel%"
if not "%launcherExitCode%"=="0" goto local_startup_failed

if /I "%~1"=="--CheckOnly" goto startup_succeeded
if /I "%~1"=="-CheckOnly" goto startup_succeeded

pushd "%~dp0syntheticIncidentGenerator"
set "GENERATOR_URL=http://localhost:8082/"
set "HEALTH_URL=%GENERATOR_URL%actuator/health"

rem Reuse an already-running generator instead of opening a second server window.
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "try { $response = Invoke-WebRequest -UseBasicParsing -Uri '%HEALTH_URL%' -TimeoutSec 2; if ($response.StatusCode -eq 200) { exit 0 } } catch {}; exit 1"
if not errorlevel 1 goto open_generator_browser

echo Starting the Synthetic Incident Generator...
start "Synthetic Incident Generator" powershell.exe -NoProfile -NoExit -ExecutionPolicy Bypass -Command "& '..\mvnw.cmd' -f .\pom.xml spring-boot:run"

echo Waiting for http://localhost:8082 to become ready...
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$deadline = (Get-Date).AddSeconds(60); do { try { $response = Invoke-WebRequest -UseBasicParsing -Uri '%HEALTH_URL%' -TimeoutSec 2; if ($response.StatusCode -eq 200) { exit 0 } } catch {}; Start-Sleep -Milliseconds 500 } while ((Get-Date) -lt $deadline); exit 1"
if errorlevel 1 goto generator_startup_failed

:open_generator_browser
echo Opening %GENERATOR_URL%
start "" "%GENERATOR_URL%"
popd

:startup_succeeded
endlocal
exit /b 0

:generator_startup_failed
echo.
echo The generator did not become ready within 60 seconds.
echo Review the Synthetic Incident Generator PowerShell window for the error.
pause
popd
endlocal
exit /b 1

:local_startup_failed
echo.
echo Local startup failed. Review the message above, then try again.
pause
endlocal & exit /b %launcherExitCode%
