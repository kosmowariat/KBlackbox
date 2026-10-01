<#
.SYNOPSIS
    Updates APKEnclave on a phone connected over adb (keeps app data). The debug variant is a separate
    app (APKEnclave Dev, applicationId ending in .debug); -Release updates the everyday app.
.PARAMETER Build
    Build the APK first (scripts\build.ps1).
.PARAMETER Release
    Use the release variant instead of debug.
.PARAMETER Launch
    Start the app after installing.
.PARAMETER Serial
    adb device serial; required only when more than one device is connected.
.EXAMPLE
    .\scripts\install.ps1 -Build -Launch
#>
[CmdletBinding()]
param(
    [switch]$Build,
    [switch]$Release,
    [switch]$Launch,
    [string]$Serial
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$variant = if ($Release) { 'release' } else { 'debug' }
$appId = if ($Release) { 'com.kosmowariat.appenclave' } else { 'com.kosmowariat.appenclave.debug' }
$launcher = "$appId/top.niunaijun.blackboxa.view.main.WelcomeActivity"

function Get-Adb {
    $sdk = $null
    $props = Join-Path $root 'local.properties'
    if (Test-Path $props) {
        $line = Get-Content $props | Where-Object { $_ -match '^\s*sdk\.dir\s*=' } | Select-Object -First 1
        $sdk = ($line -split '=', 2)[1].Trim().Replace('\:', ':').Replace('\\', '\')
    }
    if (-not $sdk) { $sdk = $env:ANDROID_HOME }
    if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
    if ($sdk) {
        $adb = Join-Path $sdk 'platform-tools\adb.exe'
        if (Test-Path $adb) { return $adb }
    }
    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    throw 'adb not found. Set sdk.dir in local.properties or ANDROID_HOME.'
}

$adb = Get-Adb
$adbArgs = @()
if ($Serial) { $adbArgs += @('-s', $Serial) }

if ($Build) {
    $buildArgs = @{}
    if ($Release) { $buildArgs.Release = $true }
    & "$PSScriptRoot\build.ps1" @buildArgs
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

# Wait briefly for a device; fail with a hint if none is authorized.
& $adb start-server | Out-Null
$devices = @(& $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '\S' })
$ready = @($devices | Where-Object { $_ -match '\sdevice$' })
if ($ready.Count -eq 0) {
    if ($devices | Where-Object { $_ -match 'unauthorized' }) {
        throw 'Device is unauthorized. Unlock the phone and accept the USB debugging prompt.'
    }
    throw 'No adb device found. Connect the phone and enable USB debugging.'
}
if ($ready.Count -gt 1 -and -not $Serial) {
    throw "More than one device connected. Pass -Serial <id>:`n$($ready -join "`n")"
}

# Pick the APK that matches the device ABI.
$abi = (& $adb @adbArgs shell getprop ro.product.cpu.abi).Trim()
$apkDir = Join-Path $root "app\build\outputs\apk\$variant"
$apk = Get-ChildItem $apkDir -Filter "*_${abi}-${variant}.apk" -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $apk) {
    $apk = Get-ChildItem $apkDir -Filter "*_universal-${variant}.apk" -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
}
if (-not $apk) {
    throw "No $variant APK for ABI '$abi' in $apkDir. Run with -Build."
}

Write-Host "Installing $($apk.Name) (device ABI: $abi)"
& $adb @adbArgs install -r $apk.FullName
if ($LASTEXITCODE -ne 0) {
    Write-Error "adb install failed (exit code $LASTEXITCODE)"
    exit $LASTEXITCODE
}

if ($Launch) {
    & $adb @adbArgs shell am start -n $launcher | Out-Null
    Write-Host 'App started.'
}
