## Native Code (Bcore/src/main/cpp)

### Build
ndk-build (`Android.mk`/`Application.mk`), not CMake. Main lib: C++17, `-fvisibility=hidden`, section GC, exceptions on; xdl: C++11, no exceptions/RTTI. `APP_STL := c++_static`, `APP_PLATFORM := android-24`, ARM ABIs only.
Link with `-z max-page-size=16384` (16KB pages) and `--strip-all`. Every new `.cpp` must be listed in `LOCAL_SRC_FILES` (don't rely on `#include`-ing .cpp files).

### JNI registration
Register natives dynamically: `static JNINativeMethod gMethods[]` + `RegisterNatives` in `JNI_OnLoad` against `top/niunaijun/blackbox/core/NativeCore`; the Java side declares `public static native` methods in `NativeCore.java`. No exported `Java_*` symbols.

### Hook modules
Each area is `class <Area>Hook : public BaseHook` in `cpp/Hook/` with static methods and `static void init(JNIEnv *env)` called from BoxCore init.
JNI replacements use `HOOK_JNI(ret, name, args...)` (declares `orig_<name>`, defines `new_<name>`) installed via `JniHook::HookJniFun(...)`; inline libc hooks keep the `orig_`/`new_` naming.

### No logging inside critical hooks
Never call logging (LOGD/ALOGD) from critical libc/native hooks — it can re-enter hooked calls and recurse infinitely. Blocked paths return `ENOENT` silently.

### Logging elsewhere
Use `ALOGD`/`ALOGE` from `cpp/Log.h` (TAG "NativeCore"), prefixing messages with the module name.

### Style
Headers use `#ifndef <PREFIX>_<NAME>_H` guards (no `#pragma once`); use `#include`, never `#import`; modules are static-method classes with camelCase methods (new free functions: camelCase too). Vendored `Dobby/` and `xdl/` are not modified.

Source: config (Android.mk, Application.mk), code, docs (RELEASE_NOTES).
