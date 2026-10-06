param([string] $Executable, [string] $ArgumentsJson, [string] $Directory)
$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'Launch.psm1') -Force
# This bootstrap already has the system's isolated environment. ShellExecute
# opens a separate console owned by the service host, inheriting only that environment.
$start = Get-VisibleProcessStartInfo -Executable $Executable -Arguments @(ConvertFrom-Json $ArgumentsJson) -Directory $Directory
$null = [Diagnostics.Process]::Start($start)
