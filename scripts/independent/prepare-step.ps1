param([string] $Directory)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $Directory
& ../../mvnw.cmd '-Dspring-boot.run.arguments=--spring.main.web-application-type=none' spring-boot:run
exit $LASTEXITCODE
