package top.niunaijun.blackboxa.remote

/** Locks the panel for a while after too many wrong tokens, so the token cannot be guessed online. */
class AuthGuard(
        private val maxFailures: Int,
        private val lockMillis: Long,
        private val now: () -> Long = System::currentTimeMillis
) {

    private var failures = 0
    private var lockedUntil = 0L

    @Synchronized
    fun isLocked() = now() < lockedUntil

    @Synchronized
    fun recordFailure() {
        failures++
        if (failures >= maxFailures) {
            lockedUntil = now() + lockMillis
            failures = 0
        }
    }

    @Synchronized
    fun recordSuccess() {
        failures = 0
    }
}
