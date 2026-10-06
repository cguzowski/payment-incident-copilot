$ErrorActionPreference = 'Stop'
$Host.UI.RawUI.WindowTitle = 'Operator console'
& npm.cmd start -- --host localhost --port 4200
exit $LASTEXITCODE
