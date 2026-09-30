<#
.SYNOPSIS
    Builds the KBlackbox app APKs with Gradle.
.PARAMETER Release
    Build the release variant instead of debug.
.PARAMETER Clean
    Run "clean" before building.
.EXAMPLE
    .\scripts\build.ps1
    .\scripts\build.ps1 -Release
#>
[CmdletBinding()]
param(
    [switch]$Release,
    [switch]$Clean
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$variant = if ($Release) { 'Release' } else { 'Debug' }
$tasks = @(":app:assemble$variant")
if ($Clean) { $tasks = @('clean') + $tasks }

Write-Host "Building $variant APKs: gradlew $($tasks -join ' ')"
& "$root\gradlew.bat" @tasks
if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle build failed (exit code $LASTEXITCODE)"
    exit $LASTEXITCODE
}

$apkDir = Join-Path $root "app\build\outputs\apk\$($variant.ToLower())"
Write-Host ""
Write-Host "Done. APKs in ${apkDir}:"
Get-ChildItem $apkDir -Filter *.apk | ForEach-Object {
    Write-Host ("  {0}  ({1:N1} MB)" -f $_.Name, ($_.Length / 1MB))
}
