param([string] $Java,[string] $Jar,[string] $Name,[string] $Hash)
$ErrorActionPreference = 'Stop'
$Host.UI.RawUI.WindowTitle = switch ($Name) { source { 'Generator' }; evaluator { 'Evaluator' }; investigation { 'Investigation API' }; default { $Name } }
$wrapper = Join-Path (Get-Location) 'mvnw.cmd'
$pom = Join-Path (Split-Path (Split-Path $Jar -Parent) -Parent) 'pom.xml'
& $wrapper '-f' $pom 'spring-boot:run' "-Dspring-boot.run.arguments=--info.system=$Name --info.release=independent-v1 --info.artifactSha256=$Hash --management.info.env.enabled=true"
exit $LASTEXITCODE
