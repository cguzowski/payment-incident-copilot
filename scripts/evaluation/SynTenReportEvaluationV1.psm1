Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$script:EvaluatorVersion = 'synten-report-eval/v1'
$script:InputSchemaVersion = 'synten-report-eval-input/v1'
$script:ResultSchemaVersion = 'synten-report-eval-result/v1'
$script:OracleVersion = 'scenario-oracle/v1'
$script:TerminalStatuses = @('AVAILABLE', 'UNAVAILABLE', 'TIMED_OUT', 'MALFORMED')
$script:MachineSignalPattern = '(?<![A-Z0-9_])[A-Z][A-Z0-9]*(?:_[A-Z0-9]+)+(?![A-Z0-9_])'

function Get-Sha256Bytes {
    param([Parameter(Mandatory)] [byte[]] $Bytes)
    return [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($Bytes)).ToLowerInvariant()
}

function Get-FileSha256Value {
    param([Parameter(Mandatory)] [string] $Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { throw "File is missing: $Path" }
    return Get-Sha256Bytes -Bytes ([IO.File]::ReadAllBytes((Resolve-Path -LiteralPath $Path).Path))
}

function Get-ObjectSha256Value {
    param([Parameter(Mandatory)] [object] $Value)
    $json = $Value | ConvertTo-Json -Depth 100 -Compress
    return Get-Sha256Bytes -Bytes ([Text.Encoding]::UTF8.GetBytes($json))
}

function Get-RequiredProperty {
    param(
        [Parameter(Mandatory)] [object] $Value,
        [Parameter(Mandatory)] [string] $Name,
        [Parameter(Mandatory)] [string] $Context
    )
    if ($Value -is [Collections.IDictionary]) {
        if (-not $Value.Contains($Name)) { throw "$Context is missing '$Name'." }
        return $Value[$Name]
    }
    $property = $Value.PSObject.Properties[$Name]
    if ($null -eq $property) { throw "$Context is missing '$Name'." }
    return $property.Value
}

function Assert-NonBlankString {
    param([object] $Value, [string] $Description)
    if ($Value -isnot [string] -or [string]::IsNullOrWhiteSpace($Value)) { throw "$Description must be non-blank." }
}

function Assert-Sha256 {
    param([object] $Value, [string] $Description)
    if ($Value -isnot [string] -or $Value -cnotmatch '^[0-9a-f]{64}$') { throw "$Description must be a lowercase SHA-256." }
}

function ConvertTo-RequiredInstant {
    param([object] $Value, [string] $Description)
    Assert-NonBlankString -Value $Value -Description $Description
    $parsed = [DateTimeOffset]::MinValue
    if (-not [DateTimeOffset]::TryParseExact(
            $Value,
            'O',
            [Globalization.CultureInfo]::InvariantCulture,
            [Globalization.DateTimeStyles]::RoundtripKind,
            [ref] $parsed)) {
        throw "$Description must use the round-trip ISO-8601 format."
    }
    return $parsed
}

function Assert-GuidString {
    param([object] $Value, [string] $Description)
    $parsed = [Guid]::Empty
    if ($Value -isnot [string] -or -not [Guid]::TryParse($Value, [ref] $parsed) -or $parsed -eq [Guid]::Empty) {
        throw "$Description must be a non-empty UUID."
    }
}

function Get-StringArray {
    param([object] $Value, [string] $Description)
    if ($null -eq $Value) { return [string[]] @() }
    $items = @($Value)
    foreach ($item in $items) { Assert-NonBlankString -Value $item -Description "$Description item" }
    if (@($items | Select-Object -Unique).Count -ne $items.Count) { throw "$Description contains duplicates." }
    return [string[]] $items
}

function Get-MachineSignals {
    param([AllowEmptyString()] [string] $Text)
    if ([string]::IsNullOrEmpty($Text)) { return [string[]] @() }
    return [string[]] @([regex]::Matches($Text, $script:MachineSignalPattern) | ForEach-Object Value | Select-Object -Unique)
}

function Get-ReportClaimEntries {
    param([Parameter(Mandatory)] [object] $Report)
    $entries = [System.Collections.Generic.List[object]]::new()
    $singleClaims = @(
        @{ Name = 'summary'; Type = 'SUMMARY' },
        @{ Name = 'probableCause'; Type = 'PROBABLE_CAUSE' },
        @{ Name = 'recommendation'; Type = 'RECOMMENDATION' }
    )
    foreach ($definition in $singleClaims) {
        $value = Get-RequiredProperty -Value $Report -Name $definition.Name -Context 'report'
        if ($null -ne $value) { $entries.Add([pscustomobject]@{ type = $definition.Type; claim = $value }) }
    }
    foreach ($definition in @(
            @{ Name = 'observations'; Type = 'OBSERVATION' },
            @{ Name = 'inferences'; Type = 'INFERENCE' },
            @{ Name = 'contradictions'; Type = 'CONTRADICTION' })) {
        $values = @(Get-RequiredProperty -Value $Report -Name $definition.Name -Context 'report')
        foreach ($value in $values) { $entries.Add([pscustomobject]@{ type = $definition.Type; claim = $value }) }
    }
    $confidence = Get-RequiredProperty -Value $Report -Name 'confidence' -Context 'report'
    $entries.Add([pscustomobject]@{ type = 'CONFIDENCE'; claim = $confidence })
    return $entries.ToArray()
}

function Get-ClaimStatement {
    param([Parameter(Mandatory)] [object] $Entry)
    if ($Entry.type -eq 'CONFIDENCE') {
        return [string] (Get-RequiredProperty -Value $Entry.claim -Name 'rationale' -Context 'report confidence')
    }
    return [string] (Get-RequiredProperty -Value $Entry.claim -Name 'statement' -Context "report $($Entry.type) claim")
}

function Get-ClaimEvidenceIds {
    param([Parameter(Mandatory)] [object] $Entry)
    return Get-StringArray -Value (Get-RequiredProperty -Value $Entry.claim -Name 'evidenceIds' -Context "report $($Entry.type) claim") -Description "$($Entry.type) evidenceIds"
}

function Get-ClaimKnowledgeIds {
    param([Parameter(Mandatory)] [object] $Entry)
    if ($Entry.type -eq 'CONFIDENCE') { return [string[]] @() }
    return Get-StringArray -Value (Get-RequiredProperty -Value $Entry.claim -Name 'knowledgeChunkIds' -Context "report $($Entry.type) claim") -Description "$($Entry.type) knowledgeChunkIds"
}

function Get-PercentileNearestRank {
    param([long[]] $SortedValues, [double] $Percentile)
    if ($SortedValues.Count -eq 0) { return $null }
    $rank = [Math]::Ceiling($Percentile * $SortedValues.Count)
    return $SortedValues[[Math]::Max(0, $rank - 1)]
}

function New-FixtureClaim {
    param([string] $Statement, [string] $EvidenceId, [string[]] $KnowledgeIds = @())
    return [ordered]@{
        statement = $Statement
        evidenceIds = @($EvidenceId)
        knowledgeChunkIds = @($KnowledgeIds)
    }
}

function New-SynTenReportEvaluationFixtureRun {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [string] $ObservableCatalogPath,
        [Parameter(Mandatory)] [string] $OraclePath
    )
    $catalog = Get-Content -LiteralPath $ObservableCatalogPath -Raw | ConvertFrom-Json
    $oracle = Get-Content -LiteralPath $OraclePath -Raw | ConvertFrom-Json
    $truthByCode = @{}
    foreach ($entry in @($oracle.scenarios)) { $truthByCode[$entry.code] = $entry.truth }
    $failureStatuses = @{ S002 = 'UNAVAILABLE'; S003 = 'TIMED_OUT'; S004 = 'MALFORMED' }
    $results = [System.Collections.Generic.List[object]]::new()
    for ($index = 0; $index -lt @($catalog.scenarios).Count; $index++) {
        $scenario = $catalog.scenarios[$index]
        $truth = $truthByCode[$scenario.code]
        $suffix = '{0:D12}' -f ($index + 1)
        $evidenceId = "41000000-0000-4000-8000-$suffix"
        $knowledgeId = "42000000-0000-4000-8000-$suffix"
        $requestedAt = [DateTimeOffset]::Parse('2026-09-30T10:00:00Z').AddSeconds($index)
        $completedAt = $requestedAt.AddMilliseconds(100 + ($index * 10))
        $status = if ($failureStatuses.ContainsKey($scenario.code)) { $failureStatuses[$scenario.code] } else { 'AVAILABLE' }
        $report = $null
        if ($status -eq 'AVAILABLE') {
            $requiredStatement = [string]::Join(' ', @($truth.requiredEvidence))
            $insufficient = $truth.expectedDisposition -eq 'INSUFFICIENT_EVIDENCE'
            $report = [ordered]@{
                disposition = $truth.expectedDisposition
                summary = New-FixtureClaim $requiredStatement $evidenceId
                observations = @(New-FixtureClaim $requiredStatement $evidenceId)
                inferences = if ($insufficient) { @() } else { @(New-FixtureClaim $truth.rootCause $evidenceId @($knowledgeId)) }
                probableCause = if ($insufficient) { $null } else { New-FixtureClaim $truth.rootCause $evidenceId @($knowledgeId) }
                confidence = [ordered]@{ level = $truth.expectedConfidence; rationale = $requiredStatement; evidenceIds = @($evidenceId) }
                recommendation = if ($insufficient) { $null } else { New-FixtureClaim $truth.recommendation $evidenceId @($knowledgeId) }
                contradictions = @()
                evidenceGaps = if ($insufficient) { @([ordered]@{ description = $truth.recommendation }) } else { @() }
            }
            if ($scenario.code -eq 'S001') {
                $report.probableCause.statement += ' FOREIGN_SIGNAL_FAILURE'
                $report.probableCause.evidenceIds = @('49000000-0000-4000-8000-000000000001')
                $report.recommendation.knowledgeChunkIds = @('49000000-0000-4000-8000-000000000002')
            }
            if ($scenario.code -eq 'S111') {
                $report.disposition = 'PROPOSED'
                $report.probableCause = New-FixtureClaim 'Synthetic fixture asserts an unsupported cause.' $evidenceId
                $report.recommendation = New-FixtureClaim 'Synthetic fixture asserts an unsupported action.' $evidenceId @($knowledgeId)
            }
        }
        $results.Add([ordered]@{
            scenarioCode = $scenario.code
            attemptId = "40000000-0000-4000-8000-$suffix"
            status = $status
            requestedAt = $requestedAt.ToString('O')
            completedAt = $completedAt.ToString('O')
            modelId = 'synthetic-report-fixture/v1'
            promptVersion = 'report-prompt/v3'
            promptHash = ('a' * 64)
            reportSchemaVersion = 'report-v1'
            reportSchemaHash = ('b' * 64)
            eligibleEvidenceIds = @($evidenceId)
            eligibleKnowledgeChunkIds = @($knowledgeId)
            report = $report
        })
    }
    return [ordered]@{
        schemaVersion = $script:InputSchemaVersion
        runId = '43000000-0000-4000-8000-000000000001'
        createdAt = '2026-09-30T12:00:00.0000000+00:00'
        observableCatalogSha256 = Get-FileSha256Value $ObservableCatalogPath
        oracleVersion = $script:OracleVersion
        oracleSha256 = Get-FileSha256Value $OraclePath
        results = $results.ToArray()
    }
}

function Invoke-SynTenReportEvaluation {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [object] $InputRun,
        [Parameter(Mandatory)] [string] $ObservableCatalogPath,
        [Parameter(Mandatory)] [string] $OraclePath
    )

    $catalogSha = Get-FileSha256Value $ObservableCatalogPath
    $oracleSha = Get-FileSha256Value $OraclePath
    $catalog = Get-Content -LiteralPath $ObservableCatalogPath -Raw | ConvertFrom-Json
    $oracle = Get-Content -LiteralPath $OraclePath -Raw | ConvertFrom-Json
    if ((Get-RequiredProperty $oracle 'version' 'oracle') -ne $script:OracleVersion) {
        throw "Unsupported oracle version."
    }
    if ((Get-RequiredProperty $InputRun 'schemaVersion' 'input run') -ne $script:InputSchemaVersion) {
        throw "Unsupported input schema version."
    }
    if ((Get-RequiredProperty $InputRun 'oracleVersion' 'input run') -ne $script:OracleVersion) {
        throw "Input oracle version mismatch."
    }
    if ((Get-RequiredProperty $InputRun 'observableCatalogSha256' 'input run') -cne $catalogSha) {
        throw "Input observable catalog hash mismatch."
    }
    if ((Get-RequiredProperty $InputRun 'oracleSha256' 'input run') -cne $oracleSha) {
        throw "Input oracle hash mismatch."
    }
    $runId = Get-RequiredProperty $InputRun 'runId' 'input run'
    Assert-GuidString $runId 'runId'
    $createdAt = ConvertTo-RequiredInstant (Get-RequiredProperty $InputRun 'createdAt' 'input run') 'createdAt'

    $catalogScenarios = @(Get-RequiredProperty $catalog 'scenarios' 'observable catalog')
    $oracleScenarios = @(Get-RequiredProperty $oracle 'scenarios' 'oracle')
    $results = @(Get-RequiredProperty $InputRun 'results' 'input run')
    if ($results.Count -ne 36) { throw "A gradeable run must contain exactly 36 scenario results." }
    if ($catalogScenarios.Count -ne 36 -or $oracleScenarios.Count -ne 36) {
        throw "The reviewed catalog and oracle must each contain exactly 36 scenarios."
    }

    $catalogByCode = @{}
    foreach ($scenario in $catalogScenarios) {
        $code = [string] (Get-RequiredProperty $scenario 'code' 'observable scenario')
        if ($catalogByCode.ContainsKey($code)) { throw "Observable catalog contains duplicate scenario '$code'." }
        $catalogByCode[$code] = $scenario
    }
    $oracleByCode = @{}
    foreach ($entry in $oracleScenarios) {
        $code = [string] (Get-RequiredProperty $entry 'code' 'oracle scenario')
        if ($oracleByCode.ContainsKey($code)) { throw "Oracle contains duplicate scenario '$code'." }
        if (-not $catalogByCode.ContainsKey($code)) { throw "Oracle contains unknown scenario '$code'." }
        $oracleByCode[$code] = $entry
    }

    $resultByCode = @{}
    foreach ($result in $results) {
        $code = [string] (Get-RequiredProperty $result 'scenarioCode' 'scenario result')
        if (-not $catalogByCode.ContainsKey($code)) { throw "Run contains unknown scenario '$code'." }
        if ($resultByCode.ContainsKey($code)) { throw "Run contains duplicate scenario '$code'." }
        $resultByCode[$code] = $result
    }
    foreach ($code in $catalogByCode.Keys) {
        if (-not $resultByCode.ContainsKey($code)) { throw "Run is missing scenario '$code'." }
    }

    $statusCounts = [ordered]@{ AVAILABLE = 0; UNAVAILABLE = 0; TIMED_OUT = 0; MALFORMED = 0 }
    $correctness = [ordered]@{ terminalAvailable = 0; dispositionCorrect = 0; confidenceCorrect = 0; requiredSignalsCovered = 0; insufficientNullContract = 0 }
    $citations = [ordered]@{ claimsRequiringCitations = 0; claimsWithRequiredCitations = 0; totalReferences = 0; validReferences = 0; unknownEvidenceReferences = 0; unknownKnowledgeReferences = 0; recommendationsRequiringKnowledge = 0; recommendationsWithKnowledge = 0 }
    $unsupported = [ordered]@{ unknownEvidenceReferences = 0; unknownKnowledgeReferences = 0; foreignSignalTokens = 0; insufficientCauseOrRecommendation = 0; totalIndicators = 0 }
    $latencies = [System.Collections.Generic.List[long]]::new()
    $scenarioGrades = [System.Collections.Generic.List[object]]::new()

    foreach ($scenario in $catalogScenarios) {
        $code = [string] $scenario.code
        $result = $resultByCode[$code]
        $truth = $oracleByCode[$code].truth
        $attemptId = Get-RequiredProperty $result 'attemptId' "result $code"
        Assert-GuidString $attemptId "$code attemptId"
        $status = [string] (Get-RequiredProperty $result 'status' "result $code")
        if ($status -cnotin $script:TerminalStatuses) { throw "$code has unsupported terminal status '$status'." }
        $statusCounts[$status]++
        $requestedAt = ConvertTo-RequiredInstant (Get-RequiredProperty $result 'requestedAt' "result $code") "$code requestedAt"
        $completedAt = ConvertTo-RequiredInstant (Get-RequiredProperty $result 'completedAt' "result $code") "$code completedAt"
        if ($completedAt -lt $requestedAt) { throw "$code completedAt precedes requestedAt." }
        $latencyMs = [long] [Math]::Round(($completedAt - $requestedAt).TotalMilliseconds)
        $latencies.Add($latencyMs)

        foreach ($name in @('modelId', 'promptVersion', 'reportSchemaVersion')) {
            Assert-NonBlankString (Get-RequiredProperty $result $name "result $code") "$code $name"
        }
        foreach ($name in @('promptHash', 'reportSchemaHash')) {
            Assert-Sha256 (Get-RequiredProperty $result $name "result $code") "$code $name"
        }
        $eligibleEvidence = @(Get-StringArray (Get-RequiredProperty $result 'eligibleEvidenceIds' "result $code") "$code eligibleEvidenceIds")
        $eligibleKnowledge = @(Get-StringArray (Get-RequiredProperty $result 'eligibleKnowledgeChunkIds' "result $code") "$code eligibleKnowledgeChunkIds")
        foreach ($identifier in @($eligibleEvidence + $eligibleKnowledge)) { Assert-GuidString $identifier "$code eligible source identifier" }
        $report = Get-RequiredProperty $result 'report' "result $code"
        if ($status -eq 'AVAILABLE' -and $null -eq $report) { throw "$code available result requires a report." }
        if ($status -ne 'AVAILABLE' -and $null -ne $report) { throw "$code failure result cannot contain a report." }

        $dispositionCorrect = $false
        $confidenceCorrect = $false
        $signalsCovered = $false
        $insufficientNullContract = $false
        $unknownEvidence = 0
        $unknownKnowledge = 0
        $foreignSignals = [System.Collections.Generic.List[string]]::new()
        $insufficientAssertions = 0
        $requiredSignals = @(Get-MachineSignals ([string]::Join(' ', @($truth.requiredEvidence))))
        $matchedSignals = [System.Collections.Generic.List[string]]::new()

        if ($status -eq 'AVAILABLE') {
            $correctness.terminalAvailable++
            $disposition = [string] (Get-RequiredProperty $report 'disposition' "report $code")
            $confidence = Get-RequiredProperty $report 'confidence' "report $code"
            $confidenceLevel = [string] (Get-RequiredProperty $confidence 'level' "report $code confidence")
            $dispositionCorrect = $disposition -ceq [string] $truth.expectedDisposition
            $confidenceCorrect = $confidenceLevel -ceq [string] $truth.expectedConfidence
            if ($dispositionCorrect) { $correctness.dispositionCorrect++ }
            if ($confidenceCorrect) { $correctness.confidenceCorrect++ }

            $claims = @(Get-ReportClaimEntries $report)
            $allStatements = [System.Collections.Generic.List[string]]::new()
            foreach ($entry in $claims) {
                $statement = Get-ClaimStatement $entry
                Assert-NonBlankString $statement "$code $($entry.type) statement"
                $allStatements.Add($statement)
                $evidenceIds = @(Get-ClaimEvidenceIds $entry)
                $knowledgeIds = @(Get-ClaimKnowledgeIds $entry)
                $citations.claimsRequiringCitations++
                if ($evidenceIds.Count -gt 0) { $citations.claimsWithRequiredCitations++ }
                if ($entry.type -eq 'RECOMMENDATION') {
                    $citations.recommendationsRequiringKnowledge++
                    if ($knowledgeIds.Count -gt 0) { $citations.recommendationsWithKnowledge++ }
                }
                foreach ($identifier in $evidenceIds) {
                    Assert-GuidString $identifier "$code claim evidence identifier"
                    $citations.totalReferences++
                    if ($identifier -cin $eligibleEvidence) { $citations.validReferences++ } else { $unknownEvidence++; $citations.unknownEvidenceReferences++ }
                }
                foreach ($identifier in $knowledgeIds) {
                    Assert-GuidString $identifier "$code claim knowledge identifier"
                    $citations.totalReferences++
                    if ($identifier -cin $eligibleKnowledge) { $citations.validReferences++ } else { $unknownKnowledge++; $citations.unknownKnowledgeReferences++ }
                }
            }
            $statementText = [string]::Join(' ', $allStatements)
            foreach ($signal in $requiredSignals) {
                if ($statementText -cmatch "(?<![A-Z0-9_])$([regex]::Escape($signal))(?![A-Z0-9_])") { $matchedSignals.Add($signal) }
            }
            $signalsCovered = $requiredSignals.Count -eq 0 -or $matchedSignals.Count -eq $requiredSignals.Count
            if ($signalsCovered) { $correctness.requiredSignalsCovered++ }

            $observableSignals = @($scenario.evidence.errors | ForEach-Object errorCode)
            foreach ($signal in @(Get-MachineSignals $statementText)) {
                if ($signal -cnotin $observableSignals) { $foreignSignals.Add($signal) }
            }
            $foreignSignals = [System.Collections.Generic.List[string]] @($foreignSignals | Select-Object -Unique)
            $unsupported.foreignSignalTokens += $foreignSignals.Count

            $probableCause = Get-RequiredProperty $report 'probableCause' "report $code"
            $recommendation = Get-RequiredProperty $report 'recommendation' "report $code"
            if ($truth.expectedDisposition -eq 'INSUFFICIENT_EVIDENCE') {
                if ($null -ne $probableCause) { $insufficientAssertions++ }
                if ($null -ne $recommendation) { $insufficientAssertions++ }
                $insufficientNullContract = $null -eq $probableCause -and $null -eq $recommendation
                if ($insufficientNullContract) { $correctness.insufficientNullContract++ }
                $unsupported.insufficientCauseOrRecommendation += $insufficientAssertions
            }
        }

        $unsupported.unknownEvidenceReferences += $unknownEvidence
        $unsupported.unknownKnowledgeReferences += $unknownKnowledge
        $scenarioGrades.Add([ordered]@{
            scenarioCode = $code
            attemptId = $attemptId
            status = $status
            requestedAt = $requestedAt.ToString('O')
            completedAt = $completedAt.ToString('O')
            latencyMs = $latencyMs
            modelId = $result.modelId
            promptVersion = $result.promptVersion
            promptHash = $result.promptHash
            reportSchemaVersion = $result.reportSchemaVersion
            reportSchemaHash = $result.reportSchemaHash
            eligibleEvidenceIds = $eligibleEvidence
            eligibleKnowledgeChunkIds = $eligibleKnowledge
            report = $report
            correctness = [ordered]@{
                terminalAvailable = $status -eq 'AVAILABLE'
                dispositionCorrect = $dispositionCorrect
                confidenceCorrect = $confidenceCorrect
                requiredSignals = $requiredSignals
                matchedRequiredSignals = $matchedSignals.ToArray()
                requiredSignalsCovered = $signalsCovered
                insufficientNullContract = $insufficientNullContract
            }
            citations = [ordered]@{
                unknownEvidenceReferences = $unknownEvidence
                unknownKnowledgeReferences = $unknownKnowledge
            }
            unsupportedClaimIndicators = [ordered]@{
                foreignSignalTokens = $foreignSignals.ToArray()
                insufficientCauseOrRecommendation = $insufficientAssertions
            }
        })
    }

    $unsupported.totalIndicators = $unsupported.unknownEvidenceReferences + $unsupported.unknownKnowledgeReferences + $unsupported.foreignSignalTokens + $unsupported.insufficientCauseOrRecommendation
    $sortedLatencies = [long[]] @($latencies | Sort-Object)
    $median = if ($sortedLatencies.Count % 2 -eq 1) {
        $sortedLatencies[[Math]::Floor($sortedLatencies.Count / 2)]
    } else {
        [long] (($sortedLatencies[($sortedLatencies.Count / 2) - 1] + $sortedLatencies[$sortedLatencies.Count / 2]) / 2)
    }
    $modulePath = $PSCommandPath
    return [ordered]@{
        schemaVersion = $script:ResultSchemaVersion
        evaluatorVersion = $script:EvaluatorVersion
        runId = $runId
        evaluatedAt = $createdAt.ToString('O')
        inputMetadata = [ordered]@{
            schemaVersion = $InputRun.schemaVersion
            createdAt = $createdAt.ToString('O')
            resultCount = $results.Count
        }
        provenance = [ordered]@{
            evaluatorVersion = $script:EvaluatorVersion
            evaluatorSha256 = Get-FileSha256Value $modulePath
            inputSha256 = Get-ObjectSha256Value $InputRun
            observableCatalogSha256 = $catalogSha
            oracleVersion = $script:OracleVersion
            oracleSha256 = $oracleSha
        }
        metricSemantics = [ordered]@{
            correctness = 'Exact terminal, disposition, confidence, required machine-signal, and insufficient-evidence null checks; not semantic entailment.'
            citations = 'Exact retained eligible-identifier membership and required citation presence.'
            unsupportedClaims = 'Unknown citations, foreign machine-signal tokens, and cause/recommendation assertions for oracle insufficient-evidence scenarios; not general entailment.'
            latency = 'Completed-at minus requested-at for every terminal attempt.'
        }
        aggregates = [ordered]@{
            scenarioCount = $scenarioGrades.Count
            terminalStatuses = $statusCounts
            correctness = $correctness
            citations = $citations
            unsupportedClaims = $unsupported
            latency = [ordered]@{
                count = $sortedLatencies.Count
                minMs = $sortedLatencies[0]
                medianMs = $median
                p95Ms = Get-PercentileNearestRank $sortedLatencies 0.95
                maxMs = $sortedLatencies[-1]
            }
        }
        scenarios = $scenarioGrades.ToArray()
    }
}

function Write-SynTenReportEvaluationArtifact {
    [CmdletBinding()]
    param([Parameter(Mandatory)] [string] $Path, [Parameter(Mandatory)] [object] $Evaluation)
    if (Test-Path -LiteralPath $Path) { throw "Evaluation artifact already exists: $Path" }
    $parent = Split-Path -Parent $Path
    if (-not [string]::IsNullOrWhiteSpace($parent) -and -not (Test-Path -LiteralPath $parent -PathType Container)) {
        $null = New-Item -ItemType Directory -Path $parent
    }
    $temporaryPath = "$Path.tmp"
    if (Test-Path -LiteralPath $temporaryPath) { throw "Temporary evaluation artifact already exists: $temporaryPath" }
    try {
        $json = $Evaluation | ConvertTo-Json -Depth 100
        [IO.File]::WriteAllText($temporaryPath, $json + [Environment]::NewLine, [Text.UTF8Encoding]::new($false))
        [IO.File]::Move($temporaryPath, $Path)
    }
    finally {
        if (Test-Path -LiteralPath $temporaryPath) { Remove-Item -LiteralPath $temporaryPath -Force }
    }
    return [ordered]@{ path = (Resolve-Path -LiteralPath $Path).Path; sha256 = Get-FileSha256Value $Path }
}

Export-ModuleMember -Function @(
    'Invoke-SynTenReportEvaluation',
    'New-SynTenReportEvaluationFixtureRun',
    'Write-SynTenReportEvaluationArtifact'
)
