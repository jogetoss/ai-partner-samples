<#
.SYNOPSIS
  Optional convenience: installs a plugin jar on a Joget DX server that runs on this machine.
  The manual route gives the same result: Settings > Manage Plugins > Upload.

.DESCRIPTION
  1. If a jar with the same file name is already installed, it is copied to <JogetHome>\wflow\app_plugins_backup.
  2. The new jar is uploaded through the console upload form (needs Node, "npm install" in the repository root,
     and Chrome), or, with -ViaCopy, copied into <JogetHome>\wflow\app_plugins, where Joget picks it up.

.PARAMETER Jar
  The jar to install.
.PARAMETER JogetHome
  The Joget installation folder. Default: the JOGET_HOME environment variable. Needed for the backup and for -ViaCopy.
.PARAMETER Base
  The Joget address with context path. Default: the JW_BASE environment variable, or http://localhost:8080/jw.
.PARAMETER ViaCopy
  Do not use a browser: copy the jar into <JogetHome>\wflow\app_plugins.

.EXAMPLE
  .\deploy-plugin.ps1 -Jar ..\01-agent-builder-tools\target\indonesian-id-tools-1.0-SNAPSHOT.jar -JogetHome "C:\Joget-DX9-Enterprise"
.EXAMPLE
  $env:JOGET_HOME = "C:\Joget-DX9-Enterprise"; .\deploy-plugin.ps1 -Jar <jar> -ViaCopy

.NOTES
  The login for the upload comes from the JW_USER and JW_PASS environment variables (default admin / admin).
#>
param(
  [Parameter(Mandatory)][string]$Jar,
  [string]$JogetHome = $env:JOGET_HOME,
  [string]$Base = $(if ($env:JW_BASE) { $env:JW_BASE } else { "http://localhost:8080/jw" }),
  [switch]$ViaCopy
)
$ErrorActionPreference = "Stop"
$Jar = (Resolve-Path $Jar).Path
$name = Split-Path $Jar -Leaf

$live = $null
if ($JogetHome) {
  if (-not (Test-Path (Join-Path $JogetHome "wflow"))) { throw "No wflow folder under $JogetHome. Check -JogetHome / JOGET_HOME." }
  $live = Join-Path $JogetHome "wflow\app_plugins\$name"
  if (Test-Path $live) {
    $backupDir = Join-Path $JogetHome "wflow\app_plugins_backup"
    New-Item -ItemType Directory -Force $backupDir | Out-Null
    $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $bak = Join-Path $backupDir ($name -replace '\.jar$', "-$stamp.jar")
    Copy-Item $live $bak
    Write-Host "Backed up the installed jar to $bak"
  }
} elseif ($ViaCopy) {
  throw "-ViaCopy needs -JogetHome <folder> (or the JOGET_HOME environment variable)."
} else {
  Write-Host "JOGET_HOME is not set: no backup of an installed jar is made."
}

if ($ViaCopy) {
  Copy-Item $Jar $live -Force
  Write-Host "Copied to $live. Joget loads it within a few seconds."
} else {
  if (-not (Get-Command node -ErrorAction SilentlyContinue)) { throw "node is not on the PATH. Install Node.js 18 or later, or use -ViaCopy, or upload the jar in the console." }
  $env:JW_BASE = $Base
  & node (Join-Path $PSScriptRoot "deploy-upload.js") $Jar
  if ($LASTEXITCODE -ne 0) { throw "The upload failed." }
}

if ($live) {
  Start-Sleep -Seconds 8
  if (Test-Path $live) { Get-Item $live | Select-Object Name, Length, LastWriteTime | Format-Table }
}
Write-Host "Check: Settings > Manage Plugins lists the plugin, and the Joget log shows no error for the bundle."
