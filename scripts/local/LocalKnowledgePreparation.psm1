Set-StrictMode -Version Latest

function Get-LocalKnowledgePreparationPlan {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)]
        [string] $RepositoryRoot
    )

    $corpusRoot = Join-Path $RepositoryRoot 'SynTen Inc\corpus'
    $mavenArguments = @(
        '-Dspring-boot.run.arguments=--spring.main.web-application-type=none'
        'spring-boot:run'
    )
    $disabledModes = [ordered]@{
        APP_KNOWLEDGE_INGESTION_ENABLED = 'false'
        APP_KNOWLEDGE_RETRIEVAL_EVALUATION_ENABLED = 'false'
        APP_KNOWLEDGE_EMBEDDING_SMOKE_TEST_ENABLED = 'false'
    }

    return @(
        [pscustomobject]@{
            Name = 'catalog'
            Description = 'Importing the validated SynTen PDF catalog'
            MavenArguments = $mavenArguments
            EnvironmentVariables = [ordered]@{
                SYNTEN_CORPUS_ROOT = $corpusRoot
                SPRING_AI_MODEL_CHAT = 'none'
                SPRING_AI_MODEL_EMBEDDING = 'none'
                APP_KNOWLEDGE_PDF_CATALOG_ENABLED = 'true'
                APP_KNOWLEDGE_PDF_BACKFILL_ENABLED = 'false'
                APP_KNOWLEDGE_INGESTION_ENABLED = $disabledModes.APP_KNOWLEDGE_INGESTION_ENABLED
                APP_KNOWLEDGE_RETRIEVAL_EVALUATION_ENABLED = $disabledModes.APP_KNOWLEDGE_RETRIEVAL_EVALUATION_ENABLED
                APP_KNOWLEDGE_EMBEDDING_SMOKE_TEST_ENABLED = $disabledModes.APP_KNOWLEDGE_EMBEDDING_SMOKE_TEST_ENABLED
            }
        }
        [pscustomobject]@{
            Name = 'embeddings'
            Description = 'Preparing all SynTen PDF embeddings with nomic-embed-text'
            MavenArguments = $mavenArguments
            EnvironmentVariables = [ordered]@{
                SYNTEN_CORPUS_ROOT = $corpusRoot
                SPRING_AI_MODEL_CHAT = 'none'
                SPRING_AI_MODEL_EMBEDDING = 'ollama'
                APP_KNOWLEDGE_PDF_CATALOG_ENABLED = 'false'
                APP_KNOWLEDGE_PDF_BACKFILL_ENABLED = 'true'
                APP_KNOWLEDGE_INGESTION_ENABLED = $disabledModes.APP_KNOWLEDGE_INGESTION_ENABLED
                APP_KNOWLEDGE_RETRIEVAL_EVALUATION_ENABLED = $disabledModes.APP_KNOWLEDGE_RETRIEVAL_EVALUATION_ENABLED
                APP_KNOWLEDGE_EMBEDDING_SMOKE_TEST_ENABLED = $disabledModes.APP_KNOWLEDGE_EMBEDDING_SMOKE_TEST_ENABLED
            }
        }
    )
}

Export-ModuleMember -Function 'Get-LocalKnowledgePreparationPlan'
