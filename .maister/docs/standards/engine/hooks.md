## Engine Hooks (Bcore fake/)

### Binder service proxy recipe
Hook a ServiceManager binder with `I<Service>Proxy extends BinderInvocationStub` in `fake/service`: the constructor calls `super(BRServiceManager.get().getService(NAME))`; `getWho()` returns `BRI<Service>Stub.get().asInterface(...)`; `inject()` calls `replaceSystemService(NAME)`; `isBadEnv()` returns false.
Register with `addInjector(new X())` in `HookManager.init()`, inside a `BuildCompat.isX()` block when the service is SDK-specific.

### Method hooks
Each hooked method is `@ProxyMethod("methodName") public static class MethodName extends MethodHook`, nested in the proxy, overriding `protected Object hook(Object who, Method method, Object[] args) throws Throwable`.
Rewrite args (package/uid/userId via `MethodParameterUtils`) and delegate with `return method.invoke(who, args);`, or return a fixed value.

### Reusable hooks and cross-cutting rewriting
Trivial hooks: `addMethodHook(new ValueMethodProxy(name, value) | new PkgMethodProxy(name) | new UidMethodProxy(name, index))` in `onBindMethod()`.
Rewriting for all methods: override `invoke()`, apply `MethodParameterUtils.replaceFirstAppPkg/replaceFirstUid(args)`, then call `super.invoke(...)`.

### Non-binder hooks
Singletons/fields that are not binders use `ClassInvocationStub`: `getWho()` reads the live instance via a BR accessor, `inject()` writes the proxy back with `_set_<field>(proxy)`, `isBadEnv()` compares the live field with `getProxyInvocation()`.
Shared hooks live in a holder class pulled in via `@ScanClass(Holder.class)`. A `getWho()` returning null means the hook is a no-op (27 legacy stubs do this).

### Null/API guards
Reflection handles (`getRealClass()`, `getWho()`) can be null on older/newer SDKs or OEM ROMs — null-check and skip the hook instead of crashing (see `global/error-handling.md`, engine vs UI split).

### SDK gating
Use `BuildCompat.isX()` (`utils/compat`) in hook/service code, including to choose versioned mirrors.
Known bug: `isTiramisu()` checks `>= 32` and `isU()` checks `>= 33` (off by one) — fix before relying on them.

Source: code (Bcore analysis: 47 BinderInvocationStub, 492 @ProxyMethod), docs (RELEASE_NOTES).
