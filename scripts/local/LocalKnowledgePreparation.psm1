Set-StrictMode -Version Latest

function Get-LocalKnowledgePreparationPlan {
    [CmdletBinding()]
    param([Parameter(Mandatory)][string] $RepositoryRoot)

    $roots = @(
        (Join-Path $RepositoryRoot 'SynTen Inc\corpus'),
        (Join-Path $RepositoryRoot 'SynTen Inc\payment-knowledge\v1')
    )
    $steps = @(
        @{ Name = 'catalog'; Root = 0; Mode = 'catalog' },
        @{ Name = 'embeddings'; Root = 0; Mode = 'embeddings' },
        @{ Name = 'payment-catalog'; Root = 1; Mode = 'catalog' },
        @{ Name = 'payment-embeddings'; Root = 1; Mode = 'embeddings' },
        @{ Name = 'readiness'; Root = 0; Mode = 'readiness' },
        @{ Name = 'payment-readiness'; Root = 1; Mode = 'readiness' }
    )
    foreach ($step in $steps) {
        [pscustomobject]@{
            Name = $step.Name
            Description = "Preparing SynTen PDF knowledge: $($step.Name)"
            MavenArguments = @(
                '-Dspring-boot.run.arguments=--spring.main.web-application-type=none',
                'spring-boot:run'
            )
            EnvironmentVariables = [ordered]@{
                SYNTEN_CORPUS_ROOT = $roots[$step.Root]
                SPRING_AI_MODEL_CHAT = 'none'
                SPRING_AI_MODEL_EMBEDDING = $(if ($step.Mode -eq 'embeddings') { 'ollama' } else { 'none' })
                APP_KNOWLEDGE_PDF_CATALOG_ENABLED = ($step.Mode -eq 'catalog').ToString().ToLowerInvariant()
                APP_KNOWLEDGE_PDF_BACKFILL_ENABLED = ($step.Mode -eq 'embeddings').ToString().ToLowerInvariant()
                APP_KNOWLEDGE_PDF_READINESS_ENABLED = ($step.Mode -eq 'readiness').ToString().ToLowerInvariant()
                APP_KNOWLEDGE_INGESTION_ENABLED = 'false'
                APP_KNOWLEDGE_RETRIEVAL_EVALUATION_ENABLED = 'false'
                APP_KNOWLEDGE_EMBEDDING_SMOKE_TEST_ENABLED = 'false'
            }
        }
    }
}

Export-ModuleMember -Function 'Get-LocalKnowledgePreparationPlan'
