## Virtual System Services & Engine API

### Service triad
A virtual system service consists of:
1. `IB<Name>Service.aidl` in `aidl/top/niunaijun/blackbox/core/system/<area>/`;
2. `B<Name>Service extends IB<Name>Service.Stub implements ISystemService` in `core/system/<area>/`, an eager singleton with `get()`, registered in `ServiceManager` under a `<NAME>_MANAGER` constant;
3. the client `B<Name>Manager extends BlackManager<IB<Name>Service>` in `fake/frameworks/`.

### AIDL conventions
Virtual-service methods take `int userId` as the last parameter; Parcelable args are marked `in`; Parcelables are declared in one-line `parcelable X;` AIDL files mirroring the entity package.

### Client wrappers
`B*Manager` methods catch `RemoteException`, log it, and return a neutral default (null/empty/false/0); they never throw to callers. Prefer `Slog` over `printStackTrace()` in new code.

### Entities
Binder data lives in `top.niunaijun.blackbox.entity[.am|.pm|.location]` as `Parcelable` classes with public plain-named fields, a `protected X(Parcel in)` constructor and `CREATOR`.

### Stub components
Manifest stubs live in `proxy/` as `Proxy<Type>` with 50 nested `P0..P49` subclasses; intent payloads use `proxy/record/Proxy*Record.create(Intent)`.
Virtual processes are named `<host>:pN`; OEM errors mistaking these for package names are harmless.

### Singletons
Engine singletons expose `public static X get()`, backed by `private static final X sX = new X()` (double-checked locking only when lazy init is required).

### Public engine API
`BlackBoxCore.get()` is the single host-side entry point; every app operation is scoped by virtual `userId` (multi-instance = separate users). Inside a virtual process read identity from `BActivityThread`.
Always check `InstallResult.success` and report the failure message.

Source: code (Bcore analysis), docs (Docs.md, README).
