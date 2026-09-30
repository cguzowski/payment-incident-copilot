$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$modulePath = Join-Path $PSScriptRoot 'SynTenReportEvaluationV1.psm1'
if (-not (Test-Path -LiteralPath $modulePath -PathType Leaf)) {
    throw "Report evaluation module is missing: $modulePath"
}
Import-Module $modulePath -Force

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$catalogPath = Join-Path $repositoryRoot 'syntheticIncidentGenerator\src\main\resources\scenarios\catalog.json'
$oraclePath = Join-Path $repositoryRoot 'syntheticIncidentGenerator\src\main\resources\scenarios\oracle.json'
$failures = [System.Collections.Generic.List[string]]::new()

function Invoke-EvaluationTest {
    param([Parameter(Mandatory)] [string] $Name, [Parameter(Mandatory)] [scriptblock] $Test)
    try {
        & $Test
        Write-Host "PASS $Name"
    }
    catch {
        $failures.Add("$Name`: $($_.Exception.Message) [$($_.ScriptStackTrace)]")
        Write-Host "FAIL $Name"
    }
}

function Assert-True {
    param([Parameter(Mandatory)] [bool] $Condition, [Parameter(Mandatory)] [string] $Message)
    if (-not $Condition) { throw $Message }
}

function Assert-Throws {
    param([Parameter(Mandatory)] [scriptblock] $Action, [Parameter(Mandatory)] [string] $MessagePattern)
    try { & $Action }
    catch {
        if ($_.Exception.Message -notlike $MessagePattern) {
            throw "Expected '$MessagePattern' but got '$($_.Exception.Message)'."
        }
        return
    }
    throw "Expected '$MessagePattern' but no error was raised."
}

function Get-FileSha256 {
    param([Parameter(Mandatory)] [string] $Path)
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}

function New-TestClaim {
    param(
        [Parameter(Mandatory)] [string] $Statement,
        [Parameter(Mandatory)] [string] $EvidenceId,
        [string[]] $KnowledgeIds = @()
    )
    return [ordered]@{
        statement = $Statement
        evidenceIds = @($EvidenceId)
        knowledgeChunkIds = @($KnowledgeIds)
    }
}

function New-CompleteTestRun {
    $catalog = Get-Content -LiteralPath $catalogPath -Raw | ConvertFrom-Json
    $oracle = Get-Content -LiteralPath $oraclePath -Raw | ConvertFrom-Json
    $truthByCode = @{}
    foreach ($entry in $oracle.scenarios) { $truthByCode[$entry.code] = $entry.truth }

    $results = [System.Collections.Generic.List[object]]::new()
    for ($index = 0; $index -lt $catalog.scenarios.Count; $index++) {
        $scenario = $catalog.scenarios[$index]
        $truth = $truthByCode[$scenario.code]
        $suffix = '{0:D12}' -f ($index + 1)
        $attemptId = "00000000-0000-4000-8000-$suffix"
        $evidenceId = "10000000-0000-4000-8000-$suffix"
        $knowledgeId = "20000000-0000-4000-8000-$suffix"
        $requestedAt = [DateTimeOffset]::Parse('2026-09-30T10:00:00Z').AddSeconds($index)
        $completedAt = $requestedAt.AddMilliseconds(100 + ($index * 10))
        $requiredStatement = [string]::Join(' ', @($truth.requiredEvidence))
        $insufficient = $truth.expectedDisposition -eq 'INSUFFICIENT_EVIDENCE'
        $report = [ordered]@{
            disposition = $truth.expectedDisposition
            summary = New-TestClaim -Statement $requiredStatement -EvidenceId $evidenceId
            observations = @(New-TestClaim -Statement $requiredStatement -EvidenceId $evidenceId)
            inferences = if ($insufficient) { @() } else { @(New-TestClaim -Statement $truth.rootCause -EvidenceId $evidenceId -KnowledgeIds @($knowledgeId)) }
            probableCause = if ($insufficient) { $null } else { New-TestClaim -Statement $truth.rootCause -EvidenceId $evidenceId -KnowledgeIds @($knowledgeId) }
            confidence = [ordered]@{
                level = $truth.expectedConfidence
                rationale = $requiredStatement
                evidenceIds = @($evidenceId)
            }
            recommendation = if ($insufficient) { $null } else { New-TestClaim -Statement $truth.recommendation -EvidenceId $evidenceId -KnowledgeIds @($knowledgeId) }
            contradictions = @()
            evidenceGaps = if ($insufficient) { @([ordered]@{ description = $truth.recommendation }) } else { @() }
        }
        $results.Add([ordered]@{
            scenarioCode = $scenario.code
            attemptId = $attemptId
            status = 'AVAILABLE'
            requestedAt = $requestedAt.ToString('O')
            completedAt = $completedAt.ToString('O')
            modelId = 'fixture-model/v1'
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
        schemaVersion = 'synten-report-eval-input/v1'
        runId = '30000000-0000-4000-8000-000000000001'
        createdAt = '2026-09-30T12:00:00.0000000+00:00'
        observableCatalogSha256 = Get-FileSha256 $catalogPath
        oracleVersion = 'scenario-oracle/v1'
        oracleSha256 = Get-FileSha256 $oraclePath
        results = $results.ToArray()
    }
}

Invoke-EvaluationTest 'gradesCompleteRunAcrossAllOracleScenarios' {
    $grade = Invoke-SynTenReportEvaluation `
        -InputRun (New-CompleteTestRun) `
        -ObservableCatalogPath $catalogPath `
        -OraclePath $oraclePath

    Assert-True ($grade.schemaVersion -eq 'synten-report-eval-result/v1') 'Result schema differs.'
    Assert-True ($grade.scenarios.Count -eq 36) 'Every reviewed scenario must be graded.'
    Assert-True ($grade.aggregates.terminalStatuses.AVAILABLE -eq 36) 'Available total differs.'
    Assert-True ($grade.aggregates.correctness.dispositionCorrect -eq 36) 'Disposition total differs.'
    Assert-True ($grade.aggregates.correctness.confidenceCorrect -eq 36) 'Confidence total differs.'
    Assert-True ($grade.aggregates.correctness.requiredSignalsCovered -eq 36) 'Signal coverage differs.'
    Assert-True ($grade.aggregates.citations.claimsWithRequiredCitations -eq $grade.aggregates.citations.claimsRequiringCitations) 'Citation coverage differs.'
    Assert-True ($grade.aggregates.unsupportedClaims.totalIndicators -eq 0) 'Clean fixture has unsupported indicators.'
    Assert-True ($grade.aggregates.latency.count -eq 36) 'Latency count differs.'
    Assert-True ($grade.provenance.evaluatorVersion -eq 'synten-report-eval/v1') 'Evaluator version differs.'
    Assert-True ($grade.provenance.observableCatalogSha256 -eq (Get-FileSha256 $catalogPath)) 'Catalog hash differs.'
    Assert-True ($grade.provenance.oracleSha256 -eq (Get-FileSha256 $oraclePath)) 'Oracle hash differs.'
    Assert-True ($grade.provenance.evaluatorSha256 -match '^[0-9a-f]{64}$') 'Evaluator hash is absent.'
}

Invoke-EvaluationTest 'rejectsIncompleteDuplicateUnknownAndVersionMismatchedRuns' {
    $missing = New-CompleteTestRun
    $missing.results = @($missing.results | Select-Object -Skip 1)
    Assert-Throws { Invoke-SynTenReportEvaluation -InputRun $missing -ObservableCatalogPath $catalogPath -OraclePath $oraclePath } '*exactly 36*'

    $duplicate = New-CompleteTestRun
    $duplicate.results[1].scenarioCode = $duplicate.results[0].scenarioCode
    Assert-Throws { Invoke-SynTenReportEvaluation -InputRun $duplicate -ObservableCatalogPath $catalogPath -OraclePath $oraclePath } '*duplicate scenario*'

    $unknown = New-CompleteTestRun
    $unknown.results[0].scenarioCode = 'S999'
    Assert-Throws { Invoke-SynTenReportEvaluation -InputRun $unknown -ObservableCatalogPath $catalogPath -OraclePath $oraclePath } '*unknown scenario*'

    $wrongVersion = New-CompleteTestRun
    $wrongVersion.schemaVersion = 'synten-report-eval-input/v2'
    Assert-Throws { Invoke-SynTenReportEvaluation -InputRun $wrongVersion -ObservableCatalogPath $catalogPath -OraclePath $oraclePath } '*input schema version*'

    $wrongHash = New-CompleteTestRun
    $wrongHash.oracleSha256 = ('f' * 64)
    Assert-Throws { Invoke-SynTenReportEvaluation -InputRun $wrongHash -ObservableCatalogPath $catalogPath -OraclePath $oraclePath } '*oracle hash*'

    $inconsistent = New-CompleteTestRun
    $inconsistent.results[0].status = 'TIMED_OUT'
    Assert-Throws { Invoke-SynTenReportEvaluation -InputRun $inconsistent -ObservableCatalogPath $catalogPath -OraclePath $oraclePath } '*failure result cannot contain a report*'
}

Invoke-EvaluationTest 'reportsCorrectnessCitationUnsupportedLatencyAndFailuresSeparately' {
    $run = New-CompleteTestRun
    $run.results[1].status = 'UNAVAILABLE'; $run.results[1].report = $null
    $run.results[2].status = 'TIMED_OUT'; $run.results[2].report = $null
    $run.results[3].status = 'MALFORMED'; $run.results[3].report = $null
    $run.results[0].report.probableCause.statement += ' FOREIGN_SIGNAL_FAILURE'
    $run.results[0].report.probableCause.evidenceIds = @('90000000-0000-4000-8000-000000000001')
    $run.results[0].report.recommendation.knowledgeChunkIds = @('90000000-0000-4000-8000-000000000002')
    $insufficient = $run.results | Where-Object scenarioCode -eq 'S111'
    $insufficient.report.disposition = 'PROPOSED'
    $insufficient.report.probableCause = New-TestClaim -Statement 'Invented cause' -EvidenceId $insufficient.eligibleEvidenceIds[0]
    $insufficient.report.recommendation = New-TestClaim -Statement 'Take action' -EvidenceId $insufficient.eligibleEvidenceIds[0] -KnowledgeIds $insufficient.eligibleKnowledgeChunkIds

    $grade = Invoke-SynTenReportEvaluation -InputRun $run -ObservableCatalogPath $catalogPath -OraclePath $oraclePath

    Assert-True ($grade.aggregates.terminalStatuses.AVAILABLE -eq 33) 'Available total differs.'
    Assert-True ($grade.aggregates.terminalStatuses.UNAVAILABLE -eq 1) 'Unavailable total differs.'
    Assert-True ($grade.aggregates.terminalStatuses.TIMED_OUT -eq 1) 'Timed-out total differs.'
    Assert-True ($grade.aggregates.terminalStatuses.MALFORMED -eq 1) 'Malformed total differs.'
    Assert-True ($grade.aggregates.latency.count -eq 36) 'Terminal failures must retain latency.'
    Assert-True ($grade.aggregates.citations.unknownEvidenceReferences -eq 1) 'Unknown evidence reference was not counted.'
    Assert-True ($grade.aggregates.citations.unknownKnowledgeReferences -eq 1) 'Unknown knowledge reference was not counted.'
    Assert-True ($grade.aggregates.unsupportedClaims.foreignSignalTokens -eq 1) 'Foreign signal was not counted.'
    Assert-True ($grade.aggregates.unsupportedClaims.insufficientCauseOrRecommendation -eq 2) 'Insufficient-evidence assertions were not counted.'
    Assert-True ($grade.aggregates.correctness.dispositionCorrect -eq 32) 'Failures and wrong disposition must remain incorrect.'
}

Invoke-EvaluationTest 'writesAtomicHashedArtifactAndRefusesOverwrite' {
    $temporaryRoot = Join-Path ([IO.Path]::GetTempPath()) ("synten-report-eval-test-" + [Guid]::NewGuid().ToString('N'))
    $null = New-Item -ItemType Directory -Path $temporaryRoot
    try {
        $target = Join-Path $temporaryRoot 'result.json'
        $grade = Invoke-SynTenReportEvaluation -InputRun (New-CompleteTestRun) -ObservableCatalogPath $catalogPath -OraclePath $oraclePath
        $written = Write-SynTenReportEvaluationArtifact -Path $target -Evaluation $grade
        Assert-True (Test-Path -LiteralPath $target -PathType Leaf) 'Artifact was not written.'
        Assert-True (-not (Test-Path -LiteralPath "$target.tmp")) 'Partial artifact remains.'
        Assert-True ($written.sha256 -eq (Get-FileSha256 $target)) 'Returned artifact hash differs.'
        Assert-Throws { Write-SynTenReportEvaluationArtifact -Path $target -Evaluation $grade } '*already exists*'
    }
    finally {
        Remove-Item -LiteralPath $temporaryRoot -Recurse -Force
    }
}

Invoke-EvaluationTest 'buildsCompleteDeterministicFixtureWithEveryTerminalCategory' {
    $fixture = New-SynTenReportEvaluationFixtureRun `
        -ObservableCatalogPath $catalogPath `
        -OraclePath $oraclePath
    Assert-True ($fixture.results.Count -eq 36) 'Fixture must cover all scenarios.'
    Assert-True (@($fixture.results | Where-Object status -eq 'AVAILABLE').Count -eq 33) 'Fixture available count differs.'
    Assert-True (@($fixture.results | Where-Object status -eq 'UNAVAILABLE').Count -eq 1) 'Fixture unavailable count differs.'
    Assert-True (@($fixture.results | Where-Object status -eq 'TIMED_OUT').Count -eq 1) 'Fixture timed-out count differs.'
    Assert-True (@($fixture.results | Where-Object status -eq 'MALFORMED').Count -eq 1) 'Fixture malformed count differs.'
    $grade = Invoke-SynTenReportEvaluation -InputRun $fixture -ObservableCatalogPath $catalogPath -OraclePath $oraclePath
    Assert-True ($grade.aggregates.unsupportedClaims.totalIndicators -ge 4) 'Fixture must demonstrate bounded unsupported indicators.'
}

Invoke-EvaluationTest 'runnerGradesInputOrFixtureWithoutNetworkOrDatabaseAccess' {
    $runnerPath = Join-Path $PSScriptRoot 'run-synten-report-evaluation-v1.ps1'
    Assert-True (Test-Path -LiteralPath $runnerPath -PathType Leaf) 'Report evaluation runner is missing.'
    $runner = Get-Content -LiteralPath $runnerPath -Raw
    foreach ($required in @('InputPath', 'SyntheticFixture', 'OutputPath', 'Invoke-SynTenReportEvaluation', 'Write-SynTenReportEvaluationArtifact')) {
        Assert-True ($runner.Contains($required)) "Runner is missing '$required'."
    }
    foreach ($forbidden in @('Invoke-RestMethod', 'Invoke-WebRequest', 'Invoke-Sqlcmd', 'psql ', 'INSERT INTO', 'ollama ')) {
        Assert-True (-not $runner.Contains($forbidden)) "Runner contains forbidden external operation '$forbidden'."
    }
}

Invoke-EvaluationTest 'repositoryVerificationRunsReportEvaluationTests' {
    $verificationModule = Get-Content -LiteralPath (Join-Path $repositoryRoot 'scripts\verification\Verification.psm1') -Raw
    Assert-True ($verificationModule.Contains('SynTenReportEvaluationV1.Tests.ps1')) 'Repository verification omits report evaluator tests.'
}

Invoke-EvaluationTest 'keepsOracleAndGraderOutOfRuntimeInputs' {
    $runtimeRoots = @(
        (Join-Path $repositoryRoot 'backend'),
        (Join-Path $repositoryRoot 'frontend'),
        (Join-Path $repositoryRoot 'syntheticIncidentGenerator\src\main')
    )
    $allowed = @(
        'syntheticIncidentGenerator\src\main\java\com\cguzowski\syntheticincidentgenerator\generation\AnswerKeyRevealService.java',
        'syntheticIncidentGenerator\src\main\java\com\cguzowski\syntheticincidentgenerator\scenario\ClasspathScenarioOracleCatalog.java',
        'syntheticIncidentGenerator\src\main\java\com\cguzowski\syntheticincidentgenerator\scenario\ScenarioOracleCatalog.java'
    )
    $violations = [System.Collections.Generic.List[string]]::new()
    foreach ($root in $runtimeRoots) {
        Get-ChildItem -LiteralPath $root -Recurse -File | Where-Object {
            $_.FullName -notmatch '[\\/](target|node_modules|dist)[\\/]' -and
            $_.Extension -in @('.java', '.ts', '.html', '.txt', '.json', '.yml', '.yaml')
        } | ForEach-Object {
            $relative = [IO.Path]::GetRelativePath($repositoryRoot, $_.FullName)
            $content = Get-Content -LiteralPath $_.FullName -Raw
            if (($content -match 'synten-report-eval') -or (($content -match 'oracle\.json|ScenarioOracleCatalog') -and $relative -notin $allowed -and $relative -notlike '*\scenarios\oracle.json')) {
                $violations.Add($relative)
            }
        }
    }
    Assert-True ($violations.Count -eq 0) ("Runtime grading/oracle dependency found: " + ($violations -join ', '))
}

if ($failures.Count -gt 0) { throw ($failures -join [Environment]::NewLine) }
Write-Host 'All SynTen report-evaluation tests passed.'
