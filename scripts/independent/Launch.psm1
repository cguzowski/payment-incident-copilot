Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-SystemEnvironment {
    param([ValidateSet('source','evaluator','investigation')][string] $Name, [hashtable] $Settings)
    $result = @{}
    $allowed = switch ($Name) {
        source { @('COPILOT_API_BASE_URL','COPILOT_API_REQUEST_TIMEOUT','SYNTHETIC_TENANT_ID','EVALUATOR_BROWSER_BASE_URL') }
        evaluator { @('COPILOT_API_BASE_URL','COPILOT_API_REQUEST_TIMEOUT','SYNTHETIC_TENANT_ID','OLLAMA_BASE_URL','COMPARISON_CHAT_MODEL','COMPARISON_TIMEOUT','COMPARISON_ARTIFACT_DIRECTORY','GENERATOR_BROWSER_ORIGINS','EVALUATOR_PORT') }
        investigation { @($Settings.Keys | Where-Object { $_ -match '^(SPRING_DATASOURCE_|REPORT_|KNOWLEDGE_|OLLAMA_|OPERATIONS_MCP_|SYNTEN_)' -and $_ -notmatch '(ORACLE|SCENARIO|EVALUATION)' }) }
    }
    foreach ($key in $allowed) { if ($Settings.ContainsKey($key)) { $result[$key] = $Settings[$key] } }
    if ($Name -eq 'evaluator' -and !$result.ContainsKey('COMPARISON_CHAT_MODEL') -and $Settings.ContainsKey('REPORT_CHAT_MODEL')) {
        $result.COMPARISON_CHAT_MODEL = $Settings.REPORT_CHAT_MODEL
    }
    if ($Name -eq 'investigation') { $result.OPERATIONS_MCP_BASE_URL = 'http://localhost:8082' }
    return $result
}

function Get-IndependentLaunchPlan {
    param([string] $ReleaseRoot)
    foreach ($spec in @(
        @{Name='source';Port=8082;Jar='target/synthetic-incident-generator-0.1.0-SNAPSHOT.jar'},
        @{Name='evaluator';Port=8083;Jar='target/incident-evaluator-0.1.0-SNAPSHOT.jar'},
        @{Name='investigation';Port=8080;Jar='backend/copilot-api/target/copilot-api-0.1.0-SNAPSHOT.jar'}
    )) {
        [pscustomobject]@{Name=$spec.Name;Port=$spec.Port;ProbeUrl="http://127.0.0.1:$($spec.Port)";Directory=(Join-Path $ReleaseRoot $spec.Name);Jar=$spec.Jar}
    }
}

function Assert-SystemIdentity {
    param([string] $Name, [object] $Info, [string] $ArtifactHash)
    if ($Info.system -ne $Name -or $Info.release -ne 'independent-v1') {
        throw "Port belongs to another $Name runtime. Stop that process and rerun start-local.bat."
    }
    if ($ArtifactHash -and $Info.artifactSha256 -ne $ArtifactHash) {
        throw "$Name is running a different artifact. Stop that process and rerun start-local.bat."
    }
}

function Start-IsolatedProcess {
    param([string] $Executable, [string[]] $Arguments, [string] $Directory, [hashtable] $Settings,
        [string] $LogPath, [switch] $Wait, [switch] $Visible)
    if ($Visible) {
        $Arguments = @('-NoProfile','-File',(Join-Path $PSScriptRoot 'open-visible.ps1'),'-Executable',$Executable,'-ArgumentsJson',(ConvertTo-Json -InputObject @($Arguments) -Compress),'-Directory',$Directory)
    }
    $start = [Diagnostics.ProcessStartInfo]::new()
    $start.FileName = $Executable
    $start.WorkingDirectory = $Directory
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.WindowStyle = [Diagnostics.ProcessWindowStyle]::Hidden
    $start.Environment.Clear()
    foreach ($key in @('PATH','SystemRoot','WINDIR','TEMP','TMP','USERPROFILE','APPDATA','LOCALAPPDATA','ProgramFiles','ProgramFiles(x86)','ProgramData','ComSpec','PATHEXT','JAVA_HOME','MAVEN_USER_HOME')) {
        $value = [Environment]::GetEnvironmentVariable($key)
        if ($value) { $start.Environment[$key] = $value }
    }
    foreach ($entry in $Settings.GetEnumerator()) { $start.Environment[$entry.Key] = [string] $entry.Value }
    foreach ($argument in $Arguments) { $start.ArgumentList.Add($argument) }
    # Service hosts own native terminal output; configuration is passed as data.
    if ($Wait -or $Visible) {
        $process = [Diagnostics.Process]::Start($start)
        $process.WaitForExit()
        if ($process.ExitCode -ne 0) { throw "Command failed with exit code $($process.ExitCode)" }
        return
    }
    return [Diagnostics.Process]::Start($start)
}

function Get-VisibleProcessStartInfo {
    param([string] $Executable, [string[]] $Arguments, [string] $Directory)
    $start = [Diagnostics.ProcessStartInfo]::new()
    $start.FileName = [IO.Path]::Combine([Environment]::GetFolderPath([Environment+SpecialFolder]::System), 'conhost.exe')
    $start.WorkingDirectory = $Directory
    $start.UseShellExecute = $true
    $start.WindowStyle = [Diagnostics.ProcessWindowStyle]::Normal
    $start.ArgumentList.Add($Executable)
    foreach ($argument in $Arguments) {
        $start.ArgumentList.Add($argument)
    }
    return $start
}

Export-ModuleMember -Function Get-SystemEnvironment,Get-IndependentLaunchPlan,Assert-SystemIdentity,Start-IsolatedProcess,Get-VisibleProcessStartInfo
