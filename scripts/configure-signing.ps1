<#
.SYNOPSIS
    Checks the release keystore and saves its settings to ~/.gradle/gradle.properties (never to the repository).
.PARAMETER Keystore
    Path to the .jks file. Keep it outside the repository and outside the JDK folder.
.PARAMETER Alias
    Key alias used when the keystore was created.
.EXAMPLE
    .\scripts\configure-signing.ps1 -Keystore C:\Users\me\.apkenclave\apkenclave-release.jks
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Keystore,
    [string]$Alias = 'apkenclave'
)

$ErrorActionPreference = 'Stop'

function Get-Keytool {
    if ($env:JAVA_HOME) {
        $keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
        if (Test-Path $keytool) { return $keytool }
    }
    $cmd = Get-Command keytool -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    throw 'keytool not found. Set JAVA_HOME to a JDK.'
}

function Read-Secret([string]$prompt) {
    $secure = Read-Host $prompt -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
}

if (-not (Test-Path $Keystore)) { throw "Keystore not found: $Keystore" }
$Keystore = (Resolve-Path $Keystore).Path
$repoRoot = Split-Path -Parent $PSScriptRoot
if ($Keystore.StartsWith($repoRoot, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'The keystore is inside the repository. Move it somewhere else first.'
}

$storePassword = Read-Secret 'Keystore password'
$keyPassword = Read-Secret 'Key password (Enter = same as keystore password)'
if (-not $keyPassword) { $keyPassword = $storePassword }

# Passwords go to keytool through environment variables, not the command line.
$env:APKENCLAVE_CHECK_STORE_PASSWORD = $storePassword
$env:APKENCLAVE_CHECK_KEY_PASSWORD = $keyPassword
try {
    & (Get-Keytool) -list -v -keystore $Keystore -alias $Alias -storepass:env APKENCLAVE_CHECK_STORE_PASSWORD -keypass:env APKENCLAVE_CHECK_KEY_PASSWORD |
        Select-String 'Alias name|Valid from|Owner|Signature algorithm|Key Algorithm|Key Size'
    if ($LASTEXITCODE -ne 0) { throw 'keytool could not open the keystore with these values.' }
}
finally {
    Remove-Item Env:APKENCLAVE_CHECK_STORE_PASSWORD, Env:APKENCLAVE_CHECK_KEY_PASSWORD -ErrorAction SilentlyContinue
}

$propertiesFile = Join-Path $env:USERPROFILE '.gradle\gradle.properties'
New-Item -ItemType Directory -Force (Split-Path $propertiesFile) | Out-Null
$lines = @()
if (Test-Path $propertiesFile) {
    $lines = @(Get-Content $propertiesFile | Where-Object { $_ -notmatch '^\s*APKENCLAVE_' })
}
$lines += "APKENCLAVE_KEYSTORE=$($Keystore.Replace('\', '/'))"
$lines += "APKENCLAVE_KEYSTORE_PASSWORD=$storePassword"
$lines += "APKENCLAVE_KEY_ALIAS=$Alias"
$lines += "APKENCLAVE_KEY_PASSWORD=$keyPassword"
Set-Content -Path $propertiesFile -Value $lines -Encoding ASCII
Write-Host "Saved signing settings to $propertiesFile"
