[CmdletBinding()]
param(
    [switch] $CheckOnly,
    [switch] $PrepareKnowledge
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-RequiredCommand {
    param(
        [Parameter(Mandatory)]
        [string] $Name,

        [Parameter(Mandatory)]
        [string] $InstallHint
    )

    $command = Get-Command -Name $Name -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($null -eq $command) {
        throw "Required command '$Name' was not found. $InstallHint"
    }

    return $command.Source
}

function Import-DotEnv {
    param(
        [Parameter(Mandatory)]
        [string] $Path
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Missing .env file. Copy .env.example to .env and add your local database credentials."
    }

    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -notmatch '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$') {
            continue
        }

        $name = $matches[1]
        $value = $matches[2].Trim()
        if ($value.Length -ge 2) {
            $hasDoubleQuotes = $value.StartsWith('"') -and $value.EndsWith('"')
            $hasSingleQuotes = $value.StartsWith("'") -and $value.EndsWith("'")
            if ($hasDoubleQuotes -or $hasSingleQuotes) {
                $value = $value.Substring(1, $value.Length - 2)
            }
        }

        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
    }
}

function Assert-RequiredEnvironmentVariables {
    param(
        [Parameter(Mandatory)]
        [string[]] $Names
    )

    foreach ($name in $Names) {
        $value = [Environment]::GetEnvironmentVariable($name, 'Process')
        if ([string]::IsNullOrWhiteSpace($value)) {
            throw "Required setting '$name' is missing or blank in .env."
        }
    }
}

function Set-DefaultEnvironmentVariable {
    param(
        [Parameter(Mandatory)]
        [string] $Name,

        [Parameter(Mandatory)]
        [string] $Value
    )

    $configuredValue = [Environment]::GetEnvironmentVariable($Name, 'Process')
    if ([string]::IsNullOrWhiteSpace($configuredValue)) {
        [Environment]::SetEnvironmentVariable($Name, $Value, 'Process')
    }
}

function Get-McpHealthUri {
    param(
        [Parameter(Mandatory)]
        [string] $BaseUrl
    )

    $parsedBaseUri = $null
    if (-not [Uri]::TryCreate($BaseUrl, [UriKind]::Absolute, [ref] $parsedBaseUri) -or
        $parsedBaseUri.Scheme -notin @('http', 'https')) {
        throw 'OPERATIONS_MCP_BASE_URL must be an absolute HTTP or HTTPS URL.'
    }

    $healthUri = [UriBuilder]::new($parsedBaseUri)
    $healthUri.Path = $parsedBaseUri.AbsolutePath.TrimEnd('/') + '/actuator/health'
    $healthUri.Query = ''
    $healthUri.Fragment = ''
    return $healthUri.Uri.AbsoluteUri
}

function Assert-PostgresReachable {
    param(
        [Parameter(Mandatory)]
        [string] $JdbcUrl
    )

    if ($JdbcUrl -notmatch '^jdbc:postgresql://(?<DatabaseHost>[^/:?#]+)(?::(?<DatabasePort>[0-9]+))?/') {
        throw 'SPRING_DATASOURCE_URL must use jdbc:postgresql://host:port/database format.'
    }

    $databaseHost = $matches['DatabaseHost']
    $databasePort = if ($matches['DatabasePort']) {
        [int] $matches['DatabasePort']
    } else {
        5432
    }

    $client = [Net.Sockets.TcpClient]::new()
    $connection = $null
    try {
        $connection = $client.BeginConnect($databaseHost, $databasePort, $null, $null)
        if (-not $connection.AsyncWaitHandle.WaitOne([TimeSpan]::FromSeconds(3))) {
            throw 'connection timed out'
        }
        $client.EndConnect($connection)
    } catch {
        throw "PostgreSQL is not reachable at ${databaseHost}:$databasePort. Start the configured local database and try again."
    } finally {
        if ($null -ne $connection) {
            $connection.AsyncWaitHandle.Close()
        }
        $client.Close()
    }
}

function Test-HttpEndpoint {
    param(
        [Parameter(Mandatory)]
        [string] $Uri
    )

    try {
        $response = Invoke-WebRequest -Uri $Uri -UseBasicParsing -TimeoutSec 2
        return $response.StatusCode -ge 200 -and $response.StatusCode -lt 400
    } catch {
        return $false
    }
}

function Wait-ForHttpEndpoint {
    param(
        [Parameter(Mandatory)]
        [string] $Name,

        [Parameter(Mandatory)]
        [string] $Uri,

        [int] $TimeoutSeconds = 90
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-HttpEndpoint -Uri $Uri) {
            Write-Host "$Name is ready."
            return
        }
        Start-Sleep -Seconds 1
    }

    throw "$Name did not become ready within $TimeoutSeconds seconds. Review its terminal for the startup error."
}

function Install-FrontendDependenciesIfNeeded {
    param(
        [Parameter(Mandatory)]
        [string] $FrontendDirectory,

        [Parameter(Mandatory)]
        [string] $NpmCommand
    )

    $dependencyLock = Join-Path $FrontendDirectory 'node_modules/.package-lock.json'
    $packageLock = Join-Path $FrontendDirectory 'package-lock.json'
    $packageJson = Join-Path $FrontendDirectory 'package.json'
    $needsInstall = -not (Test-Path -LiteralPath $dependencyLock -PathType Leaf)

    if (-not $needsInstall) {
        $installedAt = (Get-Item -LiteralPath $dependencyLock).LastWriteTimeUtc
        $needsInstall = (Get-Item -LiteralPath $packageLock).LastWriteTimeUtc -gt $installedAt -or
            (Get-Item -LiteralPath $packageJson).LastWriteTimeUtc -gt $installedAt
    }

    if (-not $needsInstall) {
        return
    }

    Write-Host 'Installing locked frontend dependencies...'
    Push-Location $FrontendDirectory
    try {
        & $NpmCommand ci
        if ($LASTEXITCODE -ne 0) {
            throw "npm ci failed with exit code $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
}

function Invoke-LocalKnowledgePreparation {
    param(
        [Parameter(Mandatory)]
        [object[]] $Plan,

        [Parameter(Mandatory)]
        [string] $MavenCommand,

        [Parameter(Mandatory)]
        [string] $BackendDirectory
    )

    $variableNames = @($Plan |
            ForEach-Object { $_.EnvironmentVariables.Keys } |
            Select-Object -Unique)
    $previousValues = @{}
    foreach ($name in $variableNames) {
        $previousValues[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
    }

    try {
        foreach ($step in $Plan) {
            foreach ($entry in $step.EnvironmentVariables.GetEnumerator()) {
                [Environment]::SetEnvironmentVariable($entry.Key, $entry.Value, 'Process')
            }

            Write-Host "$($step.Description)..."
            Push-Location -LiteralPath $BackendDirectory
            try {
                & $MavenCommand @($step.MavenArguments)
                if ($LASTEXITCODE -ne 0) {
                    throw "Local knowledge preparation step '$($step.Name)' failed with exit code $LASTEXITCODE."
                }
            } finally {
                Pop-Location
            }
        }
    } finally {
        foreach ($name in $variableNames) {
            [Environment]::SetEnvironmentVariable($name, $previousValues[$name], 'Process')
        }
    }
}

function Start-LocalApplication {
    $repositoryRoot = Split-Path -Parent $PSScriptRoot
    $mcpDirectory = Join-Path $repositoryRoot 'backend/operations-mcp-server'
    $backendDirectory = Join-Path $repositoryRoot 'backend/copilot-api'
    $frontendDirectory = Join-Path $repositoryRoot 'frontend/operator-console'
    $knowledgePreparationModule = Join-Path $repositoryRoot 'scripts/local/LocalKnowledgePreparation.psm1'
    Import-Module $knowledgePreparationModule -Force

    Import-DotEnv -Path (Join-Path $repositoryRoot '.env')
    Set-DefaultEnvironmentVariable -Name 'OPERATIONS_MCP_BASE_URL' -Value 'http://localhost:8081'
    Set-DefaultEnvironmentVariable -Name 'OPERATIONS_MCP_REQUEST_TIMEOUT' -Value '5s'
    Assert-RequiredEnvironmentVariables -Names @(
        'SPRING_DATASOURCE_URL',
        'SPRING_DATASOURCE_USERNAME',
        'SPRING_DATASOURCE_PASSWORD',
        'OPERATIONS_MCP_BASE_URL',
        'OPERATIONS_MCP_REQUEST_TIMEOUT'
    )

    $mcpHealthUri = Get-McpHealthUri -BaseUrl $env:OPERATIONS_MCP_BASE_URL

    $javaCommand = Get-RequiredCommand -Name 'java.exe' -InstallHint 'Install Java 21 and add it to PATH.'
    $mavenCommand = Get-RequiredCommand -Name 'mvn.cmd' -InstallHint 'Install Maven 3.9+ and add it to PATH.'
    $null = Get-RequiredCommand -Name 'node.exe' -InstallHint 'Install a Node.js version supported by the Angular project.'
    $npmCommand = Get-RequiredCommand -Name 'npm.cmd' -InstallHint 'Install Node.js and npm, then add them to PATH.'

    $javaVersion = (& $javaCommand --version 2>&1) -join ' '
    if ($LASTEXITCODE -ne 0 -or $javaVersion -notmatch '^(?:openjdk|java) 21(?:[.\-+\s]|$)') {
        throw 'Java 21 is required. Check java -version and your PATH.'
    }

    Assert-PostgresReachable -JdbcUrl $env:SPRING_DATASOURCE_URL
    Install-FrontendDependenciesIfNeeded -FrontendDirectory $frontendDirectory -NpmCommand $npmCommand

    if ($CheckOnly) {
        Write-Host 'Local startup preflight passed: MCP configuration, tools, PostgreSQL, and frontend dependencies are ready.'
        return
    }

    if ($PrepareKnowledge) {
        $preparationPlan = @(Get-LocalKnowledgePreparationPlan -RepositoryRoot $repositoryRoot)
        Invoke-LocalKnowledgePreparation `
            -Plan $preparationPlan `
            -MavenCommand $mavenCommand `
            -BackendDirectory $backendDirectory
        Write-Host 'Local SynTen PDF knowledge is ready.'
    }

    if (Test-HttpEndpoint -Uri $mcpHealthUri) {
        Write-Host 'Operations MCP server is already running.'
    } else {
        Write-Host 'Starting the operations MCP server in a new terminal...'
        Start-Process -FilePath $env:ComSpec -ArgumentList '/k', 'mvn spring-boot:run' `
            -WorkingDirectory $mcpDirectory -WindowStyle Normal
        Wait-ForHttpEndpoint -Name 'Operations MCP server' -Uri $mcpHealthUri
    }

    $apiHealthUri = 'http://localhost:8080/actuator/health'
    if (Test-HttpEndpoint -Uri $apiHealthUri) {
        Write-Host 'Copilot API is already running.'
    } else {
        Write-Host 'Starting the copilot API in a new terminal...'
        Start-Process -FilePath $env:ComSpec -ArgumentList '/k', 'mvn spring-boot:run' `
            -WorkingDirectory $backendDirectory -WindowStyle Normal
        Wait-ForHttpEndpoint -Name 'Copilot API' -Uri $apiHealthUri
    }

    $operatorConsoleUri = 'http://localhost:4200'
    if (Test-HttpEndpoint -Uri $operatorConsoleUri) {
        Write-Host 'Operator console is already running.'
    } else {
        Write-Host 'Starting the operator console in a new terminal...'
        Start-Process -FilePath $env:ComSpec -ArgumentList '/k', 'npm start' `
            -WorkingDirectory $frontendDirectory -WindowStyle Normal
        Wait-ForHttpEndpoint -Name 'Operator console' -Uri $operatorConsoleUri
    }

    Start-Process $operatorConsoleUri
    Write-Host 'Payment Incident Copilot is available at http://localhost:4200.'
    Write-Host 'Close the MCP, API, and operator-console terminals, or press Ctrl+C in each, to stop the application.'
}

try {
    Start-LocalApplication
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
