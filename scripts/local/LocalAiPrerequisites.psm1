Set-StrictMode -Version Latest

function Assert-LocalOllamaModels {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)]
        [string] $BaseUrl,

        [Parameter(Mandatory)]
        [string[]] $RequiredModels,

        [scriptblock] $InventoryRequest = {
            param($Uri)
            Invoke-RestMethod -Uri $Uri -Method Get -TimeoutSec 5
        }
    )

    $parsedBaseUri = $null
    if (-not [Uri]::TryCreate($BaseUrl, [UriKind]::Absolute, [ref] $parsedBaseUri) -or
        $parsedBaseUri.Scheme -notin @('http', 'https')) {
        throw 'OLLAMA_BASE_URL must be an absolute HTTP or HTTPS URL.'
    }

    $inventoryUri = [UriBuilder]::new($parsedBaseUri)
    $inventoryUri.Path = $parsedBaseUri.AbsolutePath.TrimEnd('/') + '/api/tags'
    $inventoryUri.Query = ''
    $inventoryUri.Fragment = ''

    try {
        $inventory = & $InventoryRequest $inventoryUri.Uri.AbsoluteUri
    } catch {
        throw "Ollama is not reachable at $BaseUrl. Start Ollama and try again."
    }

    $installedModels = @($inventory.models | ForEach-Object { $_.name })
    foreach ($requiredModel in $RequiredModels) {
        $latestAlias = "${requiredModel}:latest"
        if ($requiredModel -notin $installedModels -and $latestAlias -notin $installedModels) {
            throw "Required Ollama model '$requiredModel' is not installed. Run: ollama pull $requiredModel"
        }
    }
}

Export-ModuleMember -Function 'Assert-LocalOllamaModels'
