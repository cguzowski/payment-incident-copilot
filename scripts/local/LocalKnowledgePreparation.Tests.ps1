$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$modulePath = Join-Path $PSScriptRoot 'LocalKnowledgePreparation.psm1'
if (-not (Test-Path -LiteralPath $modulePath -PathType Leaf)) {
    throw "Local knowledge-preparation module is missing: $modulePath"
}
Import-Module $modulePath -Force

function Assert-Equal {
    param(
        [Parameter(Mandatory)] $Expected,
        [Parameter(Mandatory)] $Actual,
        [Parameter(Mandatory)] [string] $Message
    )

    $expectedText = $Expected -join ','
    $actualText = $Actual -join ','
    if ($expectedText -ne $actualText) {
        throw "$Message Expected '$expectedText' but got '$actualText'."
    }
}

$repositoryRoot = 'C:\synthetic\payment-incident-copilot'
$plan = @(Get-LocalKnowledgePreparationPlan -RepositoryRoot $repositoryRoot)

Assert-Equal 2 $plan.Count 'Preparation step count differs.'
Assert-Equal @('catalog', 'embeddings') $plan.Name 'Preparation order differs.'

$expectedCorpusRoot = Join-Path $repositoryRoot 'SynTen Inc\corpus'
$catalog = $plan[0]
$embeddings = $plan[1]

Assert-Equal $expectedCorpusRoot $catalog.EnvironmentVariables.SYNTEN_CORPUS_ROOT `
    'Catalog corpus root differs.'
Assert-Equal 'none' $catalog.EnvironmentVariables.SPRING_AI_MODEL_CHAT `
    'Catalog chat provider must be disabled.'
Assert-Equal 'none' $catalog.EnvironmentVariables.SPRING_AI_MODEL_EMBEDDING `
    'Catalog embedding provider must be disabled.'
Assert-Equal 'true' $catalog.EnvironmentVariables.APP_KNOWLEDGE_PDF_CATALOG_ENABLED `
    'Catalog command must be enabled.'
Assert-Equal 'false' $catalog.EnvironmentVariables.APP_KNOWLEDGE_PDF_BACKFILL_ENABLED `
    'Backfill must be disabled during catalog import.'

Assert-Equal $expectedCorpusRoot $embeddings.EnvironmentVariables.SYNTEN_CORPUS_ROOT `
    'Embedding corpus root differs.'
Assert-Equal 'none' $embeddings.EnvironmentVariables.SPRING_AI_MODEL_CHAT `
    'Embedding chat provider must be disabled.'
Assert-Equal 'ollama' $embeddings.EnvironmentVariables.SPRING_AI_MODEL_EMBEDDING `
    'Embedding provider differs.'
Assert-Equal 'false' $embeddings.EnvironmentVariables.APP_KNOWLEDGE_PDF_CATALOG_ENABLED `
    'Catalog import must be disabled during backfill.'
Assert-Equal 'true' $embeddings.EnvironmentVariables.APP_KNOWLEDGE_PDF_BACKFILL_ENABLED `
    'Backfill command must be enabled.'

$expectedArguments = @(
    '-Dspring-boot.run.arguments=--spring.main.web-application-type=none'
    'spring-boot:run'
)
foreach ($step in $plan) {
    Assert-Equal $expectedArguments $step.MavenArguments `
        "Maven arguments differ for $($step.Name)."
    foreach ($disabledMode in @(
            'APP_KNOWLEDGE_INGESTION_ENABLED',
            'APP_KNOWLEDGE_RETRIEVAL_EVALUATION_ENABLED',
            'APP_KNOWLEDGE_EMBEDDING_SMOKE_TEST_ENABLED'
        )) {
        Assert-Equal 'false' $step.EnvironmentVariables[$disabledMode] `
            "$disabledMode must be disabled for $($step.Name)."
    }
}

Write-Host 'All local knowledge-preparation tests passed.'
