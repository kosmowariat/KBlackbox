## Error Handling

### Clear User Messages
Show helpful, actionable messages without exposing internal details or security-sensitive information.

### Fail Fast
Validate inputs and check preconditions early; reject invalid data before it causes deeper issues.

### Typed Exceptions
Use specific exception types instead of generic ones to enable precise error handling.

### Centralized Handling
Catch and process errors at appropriate boundaries (controllers, API layers) rather than scattering try-catch throughout.

### Graceful Degradation
When non-critical services fail, continue operating with reduced functionality rather than crashing entirely.

### Retry with Backoff
Use exponential backoff for transient failures when calling external services.

### Resource Cleanup
Always release resources (file handles, connections) in finally blocks or equivalent cleanup mechanisms.

### Engine vs UI split (this project)
Bcore engine (hooks, proxies, `B*Manager` clients): a hook must never crash the guest app — catch, log via `Slog` with the class TAG, and fall back to the original call (`method.invoke(who, args)` / `super.invoke(...)`) or a neutral default. Null/API-check hidden-API handles and skip the hook if absent; `HookManager` wraps each `injectHook()`. Prefer logging over bare `printStackTrace()`.
App UI: fail visibly — no blanket `try/catch`; map expected failures to user-facing messages from string resources (see `frontend/android-architecture.md`). The generic principles above still apply.
