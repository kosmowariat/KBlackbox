package top.niunaijun.blackboxa.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files

class DataDirCopierTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun File.write(relative: String, text: String) {
        File(this, relative).apply { parentFile.mkdirs() }.writeText(text)
    }

    @Test
    fun copiesFilesAndNestedDirectories() {
        val source = tmp.newFolder("source")
        source.write("databases/app.db", "db")
        source.write("shared_prefs/settings.xml", "prefs")
        source.write("files/a/b/deep.txt", "deep")
        val target = File(tmp.root, "target")

        DataDirCopier.copy(source, target)

        assertEquals("db", File(target, "databases/app.db").readText())
        assertEquals("prefs", File(target, "shared_prefs/settings.xml").readText())
        assertEquals("deep", File(target, "files/a/b/deep.txt").readText())
    }

    @Test
    fun skipsNativeLibsAndCaches() {
        val source = tmp.newFolder("source")
        source.write("lib/libfoo.so", "so")
        source.write("cache/tmp", "tmp")
        source.write("code_cache/dex", "dex")
        source.write("files/keep.txt", "keep")
        val target = File(tmp.root, "target")

        DataDirCopier.copy(source, target)

        assertFalse(File(target, "lib").exists())
        assertFalse(File(target, "cache").exists())
        assertFalse(File(target, "code_cache").exists())
        assertTrue(File(target, "files/keep.txt").exists())
    }

    @Test
    fun doesNotFollowSymbolicLinks() {
        val source = tmp.newFolder("source")
        val outside = tmp.newFolder("outside")
        outside.write("secret.txt", "secret")
        source.write("files/keep.txt", "keep")
        val link = File(source, "linked")
        val created = runCatching { Files.createSymbolicLink(link.toPath(), outside.toPath()) }.isSuccess
        assumeTrue("Symbolic links are not available on this machine", created)
        val target = File(tmp.root, "target")

        DataDirCopier.copy(source, target)

        assertFalse(File(target, "linked").exists())
        assertTrue(File(target, "files/keep.txt").exists())
    }

    @Test
    fun overwritesExistingTargetFiles() {
        val source = tmp.newFolder("source")
        source.write("files/note.txt", "new")
        val target = tmp.newFolder("target")
        target.write("files/note.txt", "old")

        DataDirCopier.copy(source, target)

        assertEquals("new", File(target, "files/note.txt").readText())
    }

    @Test
    fun missingSourceDoesNothing() {
        val target = File(tmp.root, "target")

        DataDirCopier.copy(File(tmp.root, "does-not-exist"), target)

        assertFalse(target.exists())
    }
}
