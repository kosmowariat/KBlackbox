package top.niunaijun.blackboxa.util

import java.io.File

/** Copies an app data directory for a duplicated user. */
object DataDirCopier {

    private val SKIPPED_DIRS = setOf("lib", "cache", "code_cache")

    /**
     * Copies everything in [source] into [target] except the directories that get regenerated
     * (native libs and caches) and symbolic links, which would point back at the source user.
     */
    fun copy(source: File, target: File) {
        val entries = source.listFiles() ?: return
        val canonicalSource = source.canonicalFile
        target.mkdirs()
        entries.filter { it.name !in SKIPPED_DIRS && it.canonicalFile == File(canonicalSource, it.name) }
                .forEach { it.copyRecursively(File(target, it.name), overwrite = true) }
    }
}
