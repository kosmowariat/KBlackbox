# Project Vision

## Overview
KBlackbox is an Android virtual-app sandbox: it installs and runs APKs inside an isolated virtual environment without really installing them on the device. It is a solo, independent fork of [ALEX5402/NewBlackbox](https://github.com/ALEX5402/NewBlackbox), which descends from the original BlackBox engine by `niunaijun`. The fork does not plan to sync with upstream.

## Current State
- **Age**: ~2–3 years of upstream history (58 commits at fork time); forked 2026-09
- **Status**: Active development; version 4.0.0 (versionCode 400)
- **Users**: End users who want to clone or sandbox apps on one device, and developers who embed `Bcore` as a library
- **Tech Stack**: Kotlin (UI app), Java (engine, reflection codegen), C/C++ via NDK (native hooks), Gradle 8.14 / AGP 8.13

## Purpose
- Run several instances of the same app, or run untrusted apps, in isolated per-user spaces.
- Spoof device identity (Android ID, device IDs, location) for virtual apps.
- Provide GMS support, a VPN toggle and Xposed-style module hosting inside the sandbox.
- Offer a reusable engine (`Bcore`) that other host apps can integrate (see `Docs.md`).

## Goals (Next 6–12 Months)
The main focus of this fork is **improving the GUI** of the `app` module:
- Modernize the look and feel. Today the app uses a flat grey `MaterialComponents.Light` theme (`#3F3F3F`), dark mode is disabled (`forceDarkAllowed=false`) and some dialogs use hard-coded English strings.
- Improve the UX of the core flows: installing apps, the app grid and multi-user pages, per-app actions, settings, fake location and GMS management.
- Make the UI layer easier to change: consistent MVVM, updated AndroidX/lifecycle libraries, strings in resources, and fewer niche third-party UI libraries.
- Keep the engine (`Bcore`) stable while the UI evolves. Change engine code only when a UI feature needs it.

## Evolution
Upstream grew from the original BlackBox engine into NewBlackbox, adding Android 14/15 compatibility, anti-detection, a VPN toggle, device-info logging and CI builds. This fork keeps that engine as its foundation and moves the product toward a polished, modern user-facing app.

---
*Generated during Maister init on 2026-09-26 from codebase analysis and user input.*
