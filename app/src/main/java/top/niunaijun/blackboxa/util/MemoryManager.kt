package top.niunaijun.blackboxa.util

import android.util.Log

object MemoryManager {

    private const val TAG = "MemoryManager"
    private const val CRITICAL_MEMORY_THRESHOLD = 0.9
    private const val HIGH_USAGE_PERCENT = 70
    private const val SKIP_ICON_USAGE_PERCENT = 75
    private const val GC_SETTLE_MS = 100L
    private const val BYTES_PER_MB = 1024 * 1024

    private fun usageRatio(): Double {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return usedMemory.toDouble() / runtime.maxMemory().toDouble()
    }

    fun isMemoryCritical(): Boolean = usageRatio() > CRITICAL_MEMORY_THRESHOLD

    fun getMemoryUsagePercentage(): Int = (usageRatio() * 100).toInt()

    fun forceGarbageCollectionIfNeeded(): Boolean {
        if (!isMemoryCritical()) {
            return false
        }
        Log.w(TAG, "Memory usage critical (${getMemoryUsagePercentage()}%), forcing garbage collection")
        System.gc()
        Thread.sleep(GC_SETTLE_MS)
        return true
    }

    fun optimizeMemoryForRecyclerView() {
        val memoryUsage = getMemoryUsagePercentage()
        if (memoryUsage > HIGH_USAGE_PERCENT) {
            Log.d(TAG, "Memory usage high ($memoryUsage%), optimizing for RecyclerView")
            System.gc()
        }
    }

    fun shouldSkipIconLoading(): Boolean = getMemoryUsagePercentage() > SKIP_ICON_USAGE_PERCENT

    fun getMemoryInfo(): String {
        val runtime = Runtime.getRuntime()
        val usedMb = (runtime.totalMemory() - runtime.freeMemory()) / BYTES_PER_MB
        return "Memory: ${usedMb}MB used / ${runtime.maxMemory() / BYTES_PER_MB}MB max (${getMemoryUsagePercentage()}%)"
    }
}
