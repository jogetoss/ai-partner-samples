<#
.SYNOPSIS
  Installs the Agent Builder API jar from 00-setup\lib into your local Maven repository, so the samples can compile.

.DESCRIPTION
  The samples declare the dependency org.joget.ai.agent:agent-builder-api:8.2.5-ALPHA (scope "provided").
  That artifact is not on any public Maven repository, so it is shipped in this folder and installed with
  "mvn install:install-file". The pom next to the jar declares no dependencies, so Maven resolves nothing else.
  Run this once per machine (and again when you receive a newer API jar).

.PARAMETER RepoLocal
  Optional. A local Maven repository folder other than the default (%USERPROFILE%\.m2\repository).

.EXAMPLE
  .\install-api-jar.ps1
.EXAMPLE
  .\install-api-jar.ps1 -RepoLocal D:\work\m2
#>
param(
  [string]$RepoLocal = ""
)
$ErrorActionPreference = "Stop"
$version = "8.2.5-ALPHA"
$jar = Join-Path $PSScriptRoot "lib\agent-builder-api-$version.jar"
$pom = Join-Path $PSScriptRoot "lib\agent-builder-api-$version.pom"
if (-not (Test-Path $jar)) { throw "Not found: $jar" }
if (-not (Test-Path $pom)) { throw "Not found: $pom" }
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { throw "mvn is not on the PATH. Install Apache Maven 3.8 or later." }

$mvnArgs = @(
  "-q", "-B",
  "org.apache.maven.plugins:maven-install-plugin:3.1.2:install-file",
  "-Dfile=$jar",
  "-DpomFile=$pom"
)
if ($RepoLocal) { $mvnArgs += "-Dmaven.repo.local=$RepoLocal" }

& mvn @mvnArgs
if ($LASTEXITCODE -ne 0) { throw "mvn install:install-file failed (exit code $LASTEXITCODE)" }
Write-Host "Installed org.joget.ai.agent:agent-builder-api:$version"
