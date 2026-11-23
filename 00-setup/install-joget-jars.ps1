<#
.SYNOPSIS
  Installs the Joget platform jars that the samples compile against into your local Maven repository.
  The jars are taken from your own Joget DX installation.

.DESCRIPTION
  The samples compile against five Joget platform artifacts (group org.joget): wflow-core, wflow-commons,
  wflow-plugin-base, wflow-wfengine and wflow-directory. The Joget Maven repository needs a login, so a
  machine without that login cannot download them. Every Joget installation already contains the jars in
  <JOGET_HOME>\apache-tomcat-*\webapps\jw\WEB-INF\lib. This script installs them from there with
  "mvn install:install-file" and a generated pom without dependencies.

  You do NOT need this script when your local Maven repository already contains these artifacts, for example
  because you built Joget from source or your Maven settings hold a login for the Joget repository.

  The version is read from the file name (wflow-core-9.1.2.jar gives 9.1.2). Build the samples with the same
  version:  mvn clean package "-Djoget.version=9.1.2"

.PARAMETER JogetHome
  The Joget installation folder. Default: the JOGET_HOME environment variable.
.PARAMETER LibDir
  Use this instead of -JogetHome to name the folder that holds the wflow-*.jar files directly.
.PARAMETER Version
  Optional. Install under this version instead of the version in the file names.
.PARAMETER RepoLocal
  Optional. A local Maven repository folder other than the default (%USERPROFILE%\.m2\repository).

.EXAMPLE
  .\install-joget-jars.ps1 -JogetHome "C:\Joget-DX9-Enterprise"
.EXAMPLE
  $env:JOGET_HOME = "C:\Joget-DX9-Enterprise"; .\install-joget-jars.ps1
#>
param(
  [string]$JogetHome = $env:JOGET_HOME,
  [string]$LibDir = "",
  [string]$Version = "",
  [string]$RepoLocal = ""
)
$ErrorActionPreference = "Stop"
$artifacts = @("wflow-core", "wflow-commons", "wflow-plugin-base", "wflow-wfengine", "wflow-directory")

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { throw "mvn is not on the PATH. Install Apache Maven 3.8 or later." }
if (-not $LibDir) {
  if (-not $JogetHome) { throw "Give -JogetHome <folder> (or set JOGET_HOME), or give -LibDir <folder with wflow-*.jar>." }
  $found = Get-ChildItem -Path $JogetHome -Directory -Filter "apache-tomcat*" -ErrorAction SilentlyContinue |
    ForEach-Object { Join-Path $_.FullName "webapps\jw\WEB-INF\lib" } | Where-Object { Test-Path $_ } | Select-Object -First 1
  if (-not $found) { throw "No apache-tomcat*\webapps\jw\WEB-INF\lib under $JogetHome. Start Joget once so the web application is unpacked, or use -LibDir." }
  $LibDir = $found
}
if (-not (Test-Path $LibDir)) { throw "Folder not found: $LibDir" }
Write-Host "Reading Joget jars from $LibDir"

$installed = ""
foreach ($a in $artifacts) {
  # wflow-core-9.1.2.jar matches; wflow-commons-jakarta-9.1.2-jakarta.jar (another artifact) does not.
  $pattern = "^" + [regex]::Escape($a) + "-\d.*\.jar$"
  $file = Get-ChildItem -Path $LibDir -Filter "$a-*.jar" | Where-Object { $_.Name -match $pattern } | Select-Object -First 1
  if (-not $file) { throw "No $a-<version>.jar in $LibDir" }
  $v = $file.Name.Substring($a.Length + 1, $file.Name.Length - $a.Length - 5)
  if ($Version) { $v = $Version }
  $mvnArgs = @(
    "-q", "-B",
    "org.apache.maven.plugins:maven-install-plugin:3.1.2:install-file",
    "-Dfile=$($file.FullName)",
    "-DgroupId=org.joget", "-DartifactId=$a", "-Dversion=$v", "-Dpackaging=jar", "-DgeneratePom=true"
  )
  if ($RepoLocal) { $mvnArgs += "-Dmaven.repo.local=$RepoLocal" }
  & mvn @mvnArgs
  if ($LASTEXITCODE -ne 0) { throw "mvn install:install-file failed for $($file.Name) (exit code $LASTEXITCODE)" }
  Write-Host "Installed org.joget:${a}:$v  (from $($file.Name))"
  $installed = $v
}
Write-Host ""
Write-Host "Done. Build the samples with:  mvn clean package `"-Djoget.version=$installed`""
