$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$modulePath = Join-Path $PSScriptRoot 'LocalAiPrerequisites.psm1'
if (-not (Test-Path -LiteralPath $modulePath -PathType Leaf)) {
    throw "Local AI prerequisite module is missing: $modulePath"
}
Import-Module $modulePath -Force

function Assert-Throws {
    param(
        [Parameter(Mandatory)] [scriptblock] $Action,
        [Parameter(Mandatory)] [string] $MessagePattern
    )

    try {
        & $Action
    } catch {
        if ($_.Exception.Message -notlike $MessagePattern) {
            throw "Expected error like '$MessagePattern' but got '$($_.Exception.Message)'."
        }
        return
    }
    throw "Expected an error like '$MessagePattern' but no error was raised."
}

$availableModels = {
    param($Uri)
    if ($Uri -ne 'http://localhost:11434/api/tags') {
        throw "Unexpected inventory URI: $Uri"
    }
    return [pscustomobject]@{
        models = @(
            [pscustomobject]@{ name = 'qwen3:8b-q4_K_M' }
            [pscustomobject]@{ name = 'nomic-embed-text:latest' }
        )
    }
}

Assert-LocalOllamaModels `
    -BaseUrl 'http://localhost:11434' `
    -RequiredModels @('qwen3:8b-q4_K_M', 'nomic-embed-text') `
    -InventoryRequest $availableModels

Assert-Throws {
    Assert-LocalOllamaModels `
        -BaseUrl 'http://localhost:11434' `
        -RequiredModels @('qwen3:8b-q4_K_M') `
        -InventoryRequest { throw 'connection refused' }
} '*Ollama is not reachable at http://localhost:11434*'

Assert-Throws {
    Assert-LocalOllamaModels `
        -BaseUrl 'http://localhost:11434' `
        -RequiredModels @('qwen3:8b-q4_K_M') `
        -InventoryRequest { [pscustomobject]@{ models = @([pscustomobject]@{ name = 'nomic-embed-text:latest' }) } }
} '*qwen3:8b-q4_K_M*ollama pull qwen3:8b-q4_K_M*'

Assert-Throws {
    Assert-LocalOllamaModels `
        -BaseUrl 'not-a-url' `
        -RequiredModels @('qwen3:8b-q4_K_M') `
        -InventoryRequest $availableModels
} '*OLLAMA_BASE_URL must be an absolute HTTP or HTTPS URL*'

Write-Host 'All local AI prerequisite tests passed.'
