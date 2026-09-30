<#
.SYNOPSIS
    Installs the Android emulator and an arm64-v8a system image, and creates an AVD for testing KBlackbox.
.DESCRIPTION
    KBlackbox ships ARM native libraries only, so the AVD uses an arm64-v8a image. On an x86_64 PC it runs
    fully emulated (no hardware acceleration) and is slow; the first boot can take several minutes.
    The script is safe to re-run: installed packages and an existing AVD are left as they are.
.PARAMETER Api
    Android API level of the system image (default 34).
.PARAMETER ImageType
    System image flavor: google_apis (default, includes Google services) or default (AOSP).
.PARAMETER Name
    AVD name (default kblackbox_arm64).
.PARAMETER Start
    Start the emulator after setup.
.EXAMPLE
    .\scripts\setup-emulator.ps1 -Start
    .\scripts\install.ps1 -Build -Launch
#>
[CmdletBinding()]
param(
    [int]$Api = 34,
    [ValidateSet('google_apis', 'default')]
    [string]$ImageType = 'google_apis',
    [string]$Name = 'kblackbox_arm64',
    [switch]$Start
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

function Get-SdkRoot {
    $props = Join-Path $root 'local.properties'
    if (Test-Path $props) {
        $line = Get-Content $props | Where-Object { $_ -match '^\s*sdk\.dir\s*=' } | Select-Object -First 1
        if ($line) { return ($line -split '=', 2)[1].Trim().Replace('\:', ':').Replace('\\', '\') }
    }
    if ($env:ANDROID_HOME) { return $env:ANDROID_HOME }
    if ($env:ANDROID_SDK_ROOT) { return $env:ANDROID_SDK_ROOT }
    throw 'Android SDK not found. Set sdk.dir in local.properties or ANDROID_HOME.'
}

$sdk = Get-SdkRoot
$sdkmanager = Join-Path $sdk 'cmdline-tools\latest\bin\sdkmanager.bat'
$avdmanager = Join-Path $sdk 'cmdline-tools\latest\bin\avdmanager.bat'
foreach ($tool in @($sdkmanager, $avdmanager)) {
    if (-not (Test-Path $tool)) {
        throw "Missing $tool. Install 'Android SDK Command-line Tools (latest)' in Android Studio's SDK Manager."
    }
}
if (-not (Get-Command java -ErrorAction SilentlyContinue) -and -not $env:JAVA_HOME) {
    throw 'Java not found. Install a JDK (21) or set JAVA_HOME; sdkmanager needs it.'
}

$image = "system-images;android-$Api;$ImageType;arm64-v8a"
$env:ANDROID_SDK_ROOT = $sdk

Write-Host "SDK: $sdk"
Write-Host 'Accepting SDK licenses...'
(1..30 | ForEach-Object { 'y' }) | & $sdkmanager --sdk_root=$sdk --licenses | Out-Null

Write-Host "Installing emulator, platform-tools and $image (large download)..."
& $sdkmanager --sdk_root=$sdk 'emulator' 'platform-tools' $image
if ($LASTEXITCODE -ne 0) {
    Write-Error "sdkmanager failed (exit code $LASTEXITCODE)"
    exit $LASTEXITCODE
}

$existing = @(& $avdmanager list avd -c)
if ($existing -contains $Name) {
    Write-Host "AVD '$Name' already exists."
} else {
    Write-Host "Creating AVD '$Name'..."
    'no' | & $avdmanager create avd --name $Name --package $image --device pixel_6
    if ($LASTEXITCODE -ne 0) {
        Write-Error "avdmanager failed (exit code $LASTEXITCODE)"
        exit $LASTEXITCODE
    }
}

$emulator = Join-Path $sdk 'emulator\emulator.exe'
if ($Start) {
    Write-Host "Starting '$Name' (first boot is slow on x86 hosts)..."
    Start-Process $emulator -ArgumentList @('-avd', $Name, '-no-snapshot-save')
} else {
    Write-Host "Done. Start it with: & '$emulator' -avd $Name"
}
Write-Host 'Then install the app with: .\scripts\install.ps1 -Build -Launch (use -Serial emulator-5554 if the phone is also connected)'
