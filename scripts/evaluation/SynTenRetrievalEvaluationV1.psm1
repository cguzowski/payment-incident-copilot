Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$script:TenantId = '8b860d80-d17f-4e6b-8c48-af35f26a4d61'
$script:EvaluationVersion = 'synten-retrieval-eval/v1'
$script:CorpusVersion = 'synten-auth-knowledge/v1'
$script:SupersededKeys = @('PL-007', 'PL-008', 'RB-022')

function Assert-FileExists {
    param(
        [Parameter(Mandatory)] [string] $Path,
        [Parameter(Mandatory)] [string] $Description
    )
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "$Description is missing: $Path"
    }
}

function Get-ShaToken {
    param([Parameter(Mandatory)] [string] $Value)
    $bytes = [Text.Encoding]::UTF8.GetBytes($Value)
    $hash = [Security.Cryptography.SHA256]::HashData($bytes)
    return ([Convert]::ToHexString($hash).ToLowerInvariant()).Substring(0, 12)
}

function New-SynTenAlertReference {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [string] $ScenarioCode,
        [Parameter(Mandatory)] [DateTimeOffset] $DetectedAt,
        [Parameter(Mandatory)] [string] $Token
    )
    if ($ScenarioCode -notmatch '^S[0-9]{3}$') {
        throw "Cannot build an alert reference from invalid scenario code '$ScenarioCode'."
    }
    if ($Token -notmatch '^[0-9a-f]{12}$') {
        throw 'Alert reference token must contain exactly 12 lowercase hexadecimal characters.'
    }
    return "sig-v1-$ScenarioCode-$($DetectedAt.ToUnixTimeSeconds())-$Token"
}

function Get-ExpectedIds {
    param(
        [Parameter(Mandatory)] [int[]] $Ranges,
        [Parameter(Mandatory)] [string] $Prefix
    )
    $result = [System.Collections.Generic.List[string]]::new()
    for ($index = 0; $index -lt $Ranges.Count; $index += 2) {
        foreach ($number in $Ranges[$index]..$Ranges[$index + 1]) {
            $result.Add(('{0}{1:D3}' -f $Prefix, $number))
        }
    }
    return $result.ToArray()
}

function Read-EvaluationCases {
    param([Parameter(Mandatory)] [string] $Path)
    $content = Get-Content -LiteralPath $Path -Raw
    foreach ($required in @(
        'Status: Approved evaluation design',
        "Evaluation version: ``$script:EvaluationVersion``",
        "Corpus version: ``$script:CorpusVersion``"
    )) {
        if (-not (($content -replace "`r`n", "`n") -split "`n").Contains($required)) {
            throw 'The reviewed retrieval-case header has drifted.'
        }
    }

    $cases = [System.Collections.Generic.List[object]]::new()
    foreach ($line in ($content -replace "`r`n", "`n") -split "`n") {
        if ($line -notmatch '^\|\s*(KQ-[0-9]{3})\s*\|') {
            continue
        }
        $cells = @($line.Trim().Trim('|').Split('|') | ForEach-Object { $_.Trim() })
        if ($cells.Count -ne 7) {
            throw "Evaluation case row '$($matches[1])' is malformed."
        }
        $scenarioIds = if ($cells[0] -eq 'KQ-023') {
            if ($cells[1] -ne 'Synthetic exclusion probe') {
                throw 'KQ-023 must remain the synthetic exclusion probe.'
            }
            @()
        }
        else {
            @($cells[1].Split(',') | ForEach-Object { $_.Trim() })
        }
        $cases.Add([pscustomobject]@{
            CaseId = $cells[0]
            ScenarioIds = $scenarioIds
            QuerySignals = $cells[2]
            PrimaryRunbookKey = $cells[3]
            SupportingPolicyKey = $cells[4]
            WeakApprovedMatchKey = if ($cells[5] -eq 'none') { $null } else { $cells[5] }
            ExpectedReportPosture = $cells[6]
        })
    }

    $expectedCases = Get-ExpectedIds -Ranges @(1, 23) -Prefix 'KQ-'
    if (($cases.CaseId -join ',') -ne ($expectedCases -join ',')) {
        throw 'Evaluation cases must contain exactly KQ-001 through KQ-023 in order.'
    }
    return $cases.ToArray()
}

function Assert-ReviewedSources {
    param(
        [Parameter(Mandatory)] $Cases,
        [Parameter(Mandatory)] $ScenarioCatalog,
        [Parameter(Mandatory)] $Manifest
    )
    if ($Manifest.corpusVersion -ne $script:CorpusVersion -or
        $Manifest.documentCount -ne 30 -or
        @($Manifest.documents).Count -ne 30) {
        throw 'The validation manifest contract has drifted.'
    }
    $superseded = @($Manifest.documents |
        Where-Object approvalStatus -eq 'SUPERSEDED' |
        Select-Object -ExpandProperty key |
        Sort-Object)
    if (($superseded -join ',') -ne ($script:SupersededKeys -join ',')) {
        throw 'The exact three reviewed superseded keys have drifted.'
    }

    $expectedScenarios = Get-ExpectedIds -Ranges @(1, 14, 101, 111, 201, 211) -Prefix 'S'
    $actualScenarios = @($ScenarioCatalog.scenarios | Select-Object -ExpandProperty code)
    if (($actualScenarios -join ',') -ne ($expectedScenarios -join ',')) {
        throw 'The generator catalog must contain the exact 36 reviewed scenarios in order.'
    }
    $covered = @($Cases | ForEach-Object { @($_.ScenarioIds) })
    $coveredText = @($covered | Sort-Object) -join ','
    $expectedScenarioText = @($expectedScenarios | Sort-Object) -join ','
    if ($covered.Count -ne 36 -or $coveredText -ne $expectedScenarioText) {
        throw 'The evaluation cases must cover every generator scenario exactly once.'
    }

    $documents = @{}
    foreach ($document in $Manifest.documents) {
        if ($documents.ContainsKey($document.key)) {
            throw "The validation manifest contains duplicate key '$($document.key)'."
        }
        $documents[$document.key] = $document
    }
    foreach ($case in $Cases) {
        foreach ($expected in @(
            @{ Key = $case.PrimaryRunbookKey; Type = 'RUNBOOK' },
            @{ Key = $case.SupportingPolicyKey; Type = 'POLICY' },
            @{ Key = $case.WeakApprovedMatchKey; Type = 'RUNBOOK' }
        )) {
            if ($null -eq $expected.Key) {
                continue
            }
            if (-not $documents.ContainsKey($expected.Key) -or
                $documents[$expected.Key].type -ne $expected.Type -or
                $documents[$expected.Key].approvalStatus -ne 'APPROVED') {
                throw "Evaluation source '$($expected.Key)' is unknown or ineligible."
            }
        }
    }
}

function New-SynTenEvaluationSeedPlan {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [string] $CasesPath,
        [Parameter(Mandatory)] [string] $ScenarioCatalogPath,
        [Parameter(Mandatory)] [string] $ValidationManifestPath,
        [Parameter(Mandatory)] [DateTimeOffset] $DetectedAt,
        [Parameter(Mandatory)] [string] $RunToken
    )
    Assert-FileExists -Path $CasesPath -Description 'Reviewed retrieval cases'
    Assert-FileExists -Path $ScenarioCatalogPath -Description 'Reviewed generator scenario catalog'
    Assert-FileExists -Path $ValidationManifestPath -Description 'PDF validation manifest'
    if ($RunToken -notmatch '^[0-9a-f]{12}$') {
        throw 'Run token must contain exactly 12 lowercase hexadecimal characters.'
    }

    $cases = Read-EvaluationCases -Path $CasesPath
    $scenarioCatalog = Get-Content -LiteralPath $ScenarioCatalogPath -Raw | ConvertFrom-Json
    $manifest = Get-Content -LiteralPath $ValidationManifestPath -Raw | ConvertFrom-Json
    Assert-ReviewedSources -Cases $cases -ScenarioCatalog $scenarioCatalog -Manifest $manifest
    $scenarios = @{}
    foreach ($scenario in $scenarioCatalog.scenarios) {
        $scenarios[$scenario.code] = $scenario
    }

    $variants = [System.Collections.Generic.List[object]]::new()
    foreach ($case in $cases) {
        foreach ($scenarioId in @($case.ScenarioIds)) {
            $scenario = $scenarios[$scenarioId]
            $token = Get-ShaToken -Value "$RunToken|$($case.CaseId)|$scenarioId"
            $variants.Add([pscustomobject]@{
                CaseId = $case.CaseId
                VariantId = $scenarioId
                ScenarioCode = $scenarioId
                ScenarioReference = New-SynTenAlertReference -ScenarioCode $scenarioId -DetectedAt $DetectedAt -Token $token
                DetectedAt = $DetectedAt.ToUniversalTime().ToString('O')
                Title = $scenario.title
                Description = $scenario.description
                Severity = $scenario.severity
                ExpectedEvidenceStatus = $scenario.evidence.availability
                ExpectedServiceName = $scenario.evidence.serviceName
                ExpectedErrors = @($scenario.evidence.errors | ForEach-Object {
                    [pscustomobject]@{ errorCode = $_.errorCode; count = [int] $_.count }
                })
            })
        }
    }

    $probeToken = Get-ShaToken -Value "$RunToken|KQ-023|S999"
    $variants.Add([pscustomobject]@{
        CaseId = 'KQ-023'
        VariantId = 'EXCLUSION'
        ScenarioCode = 'S999'
        ScenarioReference = New-SynTenAlertReference -ScenarioCode 'S999' -DetectedAt $DetectedAt -Token $probeToken
        DetectedAt = $DetectedAt.ToUniversalTime().ToString('O')
        Title = 'Synthetic legacy knowledge exclusion probe'
        Description = 'Synthetic exclusion probe for legacy gateway recovery, emergency routing, and AI incident automation.'
        Severity = 'LOW'
        ExpectedEvidenceStatus = 'NOT_FOUND'
        ExpectedServiceName = $null
        ExpectedErrors = @()
    })

    return [pscustomobject]@{
        SchemaVersion = 'synten-retrieval-eval-seed-plan/v1'
        EvaluationVersion = $script:EvaluationVersion
        CorpusVersion = $script:CorpusVersion
        TenantId = $script:TenantId
        DetectedAt = $DetectedAt.ToUniversalTime().ToString('O')
        Variants = $variants.ToArray()
    }
}

function Assert-EqualValue {
    param(
        $Expected,
        $Actual,
        [Parameter(Mandatory)] [string] $Description
    )
    if ([string] $Expected -cne [string] $Actual) {
        throw "$Description mismatch: expected '$Expected' but got '$Actual'."
    }
}

function Assert-SynTenSeedReadback {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] $Variant,
        [Parameter(Mandatory)] [string] $TenantId,
        [Parameter(Mandatory)] $Incident,
        [Parameter(Mandatory)] $Investigation,
        [Parameter(Mandatory)] $Evidence
    )
    Assert-EqualValue -Expected $TenantId -Actual $Incident.tenantId -Description 'Persisted tenant'
    Assert-EqualValue -Expected $Variant.ScenarioReference -Actual $Incident.externalAlertId -Description 'Persisted scenario reference'
    Assert-EqualValue -Expected $Variant.Title -Actual $Incident.title -Description 'Persisted title'
    Assert-EqualValue -Expected $Variant.Description -Actual $Incident.description -Description 'Persisted description'
    Assert-EqualValue -Expected $Variant.Severity -Actual $Incident.severity -Description 'Persisted severity'
    Assert-EqualValue -Expected $Incident.incidentId -Actual $Investigation.incidentId -Description 'Persisted investigation incident'
    Assert-EqualValue -Expected $Variant.ExpectedEvidenceStatus -Actual $Evidence.status -Description 'Persisted evidence status'

    if ($Variant.ExpectedEvidenceStatus -in @('AVAILABLE', 'PARTIAL')) {
        if ($null -eq $Evidence.content) {
            throw 'Persisted evidence content is missing.'
        }
        Assert-EqualValue -Expected $Variant.ExpectedServiceName -Actual $Evidence.content.serviceName -Description 'Persisted evidence service'
        $expectedErrors = @($Variant.ExpectedErrors |
            Sort-Object errorCode |
            ForEach-Object { "$($_.errorCode):$($_.count)" }) -join ','
        $actualErrors = @($Evidence.content.errors |
            Sort-Object errorCode |
            ForEach-Object { "$($_.errorCode):$($_.count)" }) -join ','
        Assert-EqualValue -Expected $expectedErrors -Actual $actualErrors -Description 'Persisted evidence error counts'
    }
    elseif ($null -ne $Evidence.content) {
        throw 'Unavailable or unmatched evidence must not contain observations.'
    }

    return [pscustomobject]@{
        CaseId = $Variant.CaseId
        VariantId = $Variant.VariantId
        ScenarioReference = $Variant.ScenarioReference
        IncidentId = [string] $Incident.incidentId
        InvestigationId = [string] $Investigation.investigationId
        EvidenceId = [string] $Evidence.evidenceId
        EvidenceStatus = [string] $Evidence.status
    }
}

function Select-SynTenEvidenceReadback {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] $HistoryResponse,
        [Parameter(Mandatory)] [string] $EvidenceId
    )
    $history = @($HistoryResponse | ForEach-Object { $_ })
    $matches = @($history | Where-Object {
        $null -ne $_ -and
        $null -ne $_.PSObject.Properties['evidenceId'] -and
        [string] $_.evidenceId -eq $EvidenceId
    })
    if ($matches.Count -ne 1) {
        throw "Persisted evidence readback is missing or duplicated for evidence '$EvidenceId'."
    }
    return $matches[0]
}

function Write-SynTenEvaluationSeedManifest {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [string] $Path,
        [Parameter(Mandatory)] $Manifest
    )
    $absolutePath = [IO.Path]::GetFullPath($Path)
    if (Test-Path -LiteralPath $absolutePath) {
        throw "Evaluation seed manifest already exists: $absolutePath"
    }
    $directory = Split-Path -Parent $absolutePath
    if (-not (Test-Path -LiteralPath $directory -PathType Container)) {
        $null = New-Item -ItemType Directory -Path $directory
    }
    $temporaryPath = "$absolutePath.tmp-$([Guid]::NewGuid().ToString('N'))"
    try {
        $Manifest | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $temporaryPath -Encoding utf8NoBOM
        Move-Item -LiteralPath $temporaryPath -Destination $absolutePath
    }
    finally {
        if (Test-Path -LiteralPath $temporaryPath) {
            Remove-Item -LiteralPath $temporaryPath -Force
        }
    }
    return $absolutePath
}

Export-ModuleMember -Function @(
    'New-SynTenAlertReference',
    'New-SynTenEvaluationSeedPlan',
    'Assert-SynTenSeedReadback',
    'Select-SynTenEvidenceReadback',
    'Write-SynTenEvaluationSeedManifest'
)
