Set-StrictMode -Version Latest

function Start-SelectedMcpProvider {
    param(
        [Parameter(Mandatory)] [string] $RepositoryRoot,
        [switch] $UseGeneratorMcp,
        [Parameter(Mandatory)] [string] $HealthUri,
        [Parameter(Mandatory)] [scriptblock] $IsHealthy,
        [Parameter(Mandatory)] [scriptblock] $StartProvider,
        [Parameter(Mandatory)] [scriptblock] $WaitForProvider
    )
    if (& $IsHealthy $HealthUri) {
        Write-Host 'Selected evidence provider is already running.'
        return
    }
    $relativeDirectory = if ($UseGeneratorMcp) { 'syntheticIncidentGenerator' } else { 'backend/operations-mcp-server' }
    Write-Host "Starting selected evidence provider ($relativeDirectory)..."
    & $StartProvider (Join-Path $RepositoryRoot $relativeDirectory)
    & $WaitForProvider $HealthUri
}

function Assert-LocalApiMcpConfiguration {
    param(
        [Parameter(Mandatory)] [string] $ExpectedBaseUrl,
        [scriptblock] $InfoRequest = {
            param($Uri)
            Invoke-RestMethod -Uri $Uri -Method Get -TimeoutSec 5
        }
    )
    $restartMessage = 'Stop the existing Copilot API and restart it with this launcher.'
    try {
        $info = & $InfoRequest 'http://localhost:8080/actuator/info'
        $actualBaseUrl = [string] $info.operationsMcp.baseUrl
        if ([string]::IsNullOrWhiteSpace($actualBaseUrl)) { throw 'Missing endpoint metadata.' }
    } catch {
        throw "The running API evidence configuration cannot be verified. $restartMessage"
    }
    $expected = [UriBuilder]::new($ExpectedBaseUrl)
    $expected.UserName = ''
    $expected.Password = ''
    $expected.Query = ''
    $expected.Fragment = ''
    if ($actualBaseUrl.TrimEnd('/') -ne $expected.Uri.AbsoluteUri.TrimEnd('/')) {
        throw "The running API uses a different evidence provider. $restartMessage"
    }
    Write-Host 'Copilot API evidence provider configuration verified.'
}

Export-ModuleMember -Function 'Start-SelectedMcpProvider', 'Assert-LocalApiMcpConfiguration'
