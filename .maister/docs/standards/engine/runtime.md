## Engine Runtime Rules

### Logging
Java engine code logs via `top.niunaijun.blackbox.utils.Slog` with `public static final String TAG = "<ClassName>"` (TAG equals the class name; legacy "Stub" TAGs exist). Tags must make logcat filtering by component possible.

### VPN and log upload removed
The host VPN mode (`ProxyVpnService`, the VPN setting, the consent prompt and the guest VPN hooks) and the crash/log upload (`LogSender`, `sendLogs`) were removed — do not reintroduce them. Logs stay on the device.

### Xposed removed
Xposed support (BXposedManagerService, AIDL, flags, UI) was removed — do not reintroduce it. Leftover Xposed UI resources in `app` (activity_xp, item_xp, XpModuleInfo, xp strings) are dead code to delete.

### Distribution as a library
Host apps consume Bcore as an AAR (`implementation fileTree(dir: "libs", include: ["*.aar"])`); Docs.md is the integration guide (partly aspirational — verify APIs against source).

Source: docs (RELEASE_NOTES, Docs.md, README), code.
