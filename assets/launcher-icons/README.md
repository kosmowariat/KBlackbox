# Launcher icons

Color variants of the launcher and splash icons. Copy the contents of a variant folder over a `res/`
folder to switch the icon (the build uses whatever is in `res/`).

- `orange/` - original icon (orange glow); the old `top.niunaijun.blackbox` app, no longer built
- `lime/` - everyday app, the release build (`app/src/main/res/`)
- `blue/` - the separate debug app, `applicationId` ending in `.debug` (`app/src/debug/res/`)

`lime/` was made from `orange/` by rotating the hue to about 80 degrees.
