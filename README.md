# APKEnclave - Virtual Engine

<p align="center">
  <img src="assets/usage.gif" alt="APKEnclave Banner" width="100%"/>
</p>

APKEnclave (package `com.kosmowariat.appenclave`) is a virtual engine that allows you to clone and run virtual applications on Android devices without installing APKs. This project works on Android 5.0 to 14.0+ and supports multiple architectures (ARM64, ARMv7, x86).

## Overview

This enhanced edition includes bug fixes, stability improvements, and Android 14+ compatibility tailored for modern devices.

### Key Features

*   **Virtual App Cloning**: Run multiple instances of applications.
*   **Sandboxed Environment**: Isolated process execution.
*   **No Root Required**: Runs entirely in userspace.
*   **Multi-Architecture**: Support for 32-bit and 64-bit apps.
*   **Device Spoofing**: Modify device information for virtual apps.
*   **Fake Location**: Spoof GPS coordinates.
*   **Spaces**: Several independent spaces, duplicated or copied between each other, with backup and restore to a zip file.
*   **Material 3 UI**: Light and dark theme, Polish and Chinese translations, launcher shortcuts.

## Requirements

*   **Android Version**: Android 5.0 (API 21) or higher.
*   **RAM**: 2GB minimum recommended.
*   **Architecture**: ARMv7a, ARM64-v8a, x86.

## Build Instructions

### Prerequisites
*   Android Studio (Arctic Fox or newer)
*   JDK 17
*   Android SDK 34+
*   NDK (Version 29.0.13846066)

### Building from Source

```bash
# Clone the repository
git clone https://github.com/your-repo/NewBlackbox.git
cd NewBlackbox

# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease
```

### Helper Scripts (Windows / PowerShell)

The `scripts/` folder wraps the common build and device workflow. Run the scripts from the repository root.

```powershell
# Build the debug APKs (add -Release for release, -Clean to clean first)
.\scripts\build.ps1

# Update the app on a phone connected over adb (data is kept)
.\scripts\install.ps1

# Build, install and start the app in one go
.\scripts\install.ps1 -Build -Launch
```

To test on an emulator instead of a phone, set one up once (downloads the emulator and an arm64-v8a system image, creates the `kblackbox_arm64` AVD):

```powershell
.\scripts\setup-emulator.ps1 -Start
.\scripts\install.ps1 -Build -Launch -Serial emulator-5554
```

The app ships ARM libraries only, so on an x86_64 PC the emulator runs fully emulated: it works, but the first boot takes minutes and it is slow.

The debug build is a separate app, **APKEnclave Dev** (blue icon, `applicationId` ending in `.debug`), so it can sit next to the everyday app (lime icon) with its own data. `install.ps1` installs the debug app; add `-Release` for the everyday one.

`install.ps1` finds `adb` through `sdk.dir` in `local.properties` (or `ANDROID_HOME`) and installs the APK matching the device ABI. Use `-Serial <id>` when more than one device is connected and `-Release` to install the release variant.

## Integration

To use BlackBox Core in your own project, add the AAR dependency:

```gradle
dependencies {
    implementation fileTree(dir: "libs", include: ["*.aar"])
}
```

Refer to `Docs.md` for detailed API documentation.

## Troubleshooting

*   **App Crashes**: Check logcat for UID mismatches or permission errors.
*   **Installation Failures**: Verify potential architecture mismatches or storage permissions.
*   **Android 15**: Ensure you are using the latest build which handles stricter security policies.

## Credits

*   **Main Developer**: ALEX502
*   **Original Framework**: VirtualApp, VirtualAPK
*   **Native Hooks**: Dobby, xDL
*   **Reflection**: BlackReflection, FreeReflection

## License

Copyright 2022 BlackBox

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## Signing the release build

Without any configuration the release build is signed with the debug key, which differs on every machine. To sign with your own key, create a keystore **outside the repository** and set four values:

| Name | Meaning |
|------|---------|
| `APKENCLAVE_KEYSTORE` | Path to the `.jks` file |
| `APKENCLAVE_KEYSTORE_PASSWORD` | Keystore password |
| `APKENCLAVE_KEY_ALIAS` | Key alias |
| `APKENCLAVE_KEY_PASSWORD` | Key password |

Locally put them in `~/.gradle/gradle.properties` (user level, never committed) or in environment variables. In GitHub Actions they come from the repository secrets `APKENCLAVE_KEYSTORE_BASE64` (the keystore, base64), `APKENCLAVE_KEYSTORE_PASSWORD`, `APKENCLAVE_KEY_ALIAS` and `APKENCLAVE_KEY_PASSWORD`; when they are missing the build falls back to the debug key.

After creating the keystore (see below), run `.\scripts\configure-signing.ps1 -Keystore <path to the .jks>`. It asks for the passwords, checks that the keystore opens and writes the four settings to `~/.gradle/gradle.properties`.

Create the keystore once (the `.apkenclave` folder must exist) and keep a backup copy (password manager and an offline copy): if the key is lost, installed copies cannot be updated and have to be reinstalled.

```
keytool -genkeypair -v -keystore %USERPROFILE%\.apkenclave\apkenclave-release.jks -alias apkenclave -keyalg RSA -keysize 4096 -validity 10000
```
