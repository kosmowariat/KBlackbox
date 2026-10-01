package top.niunaijun.blackboxa.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthGuardTest {

    private var time = 0L
    private val guard = AuthGuard(maxFailures = 3, lockMillis = 1000) { time }

    @Test
    fun locksAfterTooManyFailuresAndUnlocksLater() {
        repeat(3) { guard.recordFailure() }
        assertTrue(guard.isLocked())
        time = 999
        assertTrue(guard.isLocked())
        time = 1000
        assertFalse(guard.isLocked())
    }

    @Test
    fun staysOpenBelowTheLimit() {
        repeat(2) { guard.recordFailure() }
        assertFalse(guard.isLocked())
    }

    @Test
    fun aSuccessResetsTheFailureCount() {
        repeat(2) { guard.recordFailure() }
        guard.recordSuccess()
        repeat(2) { guard.recordFailure() }
        assertFalse(guard.isLocked())
    }
}
