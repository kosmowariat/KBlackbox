package top.niunaijun.blackboxa.util

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SpaceBackupTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun file(path: String, text: String): File =
            File(temp.root, path).apply { parentFile?.mkdirs() }.also { it.writeText(text) }

    private fun backupWithOneApp(): File {
        val apk = file("source/app.apk", "apk-bytes")
        val data = File(temp.root, "source/data").apply { mkdirs() }
        file("source/data/save.txt", "level 7")
        file("source/data/sub/inner.txt", "inner")
        file("source/data/cache/skipped.txt", "cache")
        file("source/data/lib/skipped.so", "lib")
        val backup = File(temp.root, "backup.zip")
        FileOutputStream(backup).use {
            SpaceBackup.write(it, "Work", "a.b,c.d", listOf(BackupApp("a.b", apk, mapOf(DataKind.DATA to data))))
        }
        return backup
    }

    @Test
    fun roundTripsManifestApkAndData() {
        SpaceBackup.Reader(backupWithOneApp()).use { reader ->
            assertEquals("Work", reader.manifest.name)
            assertEquals("a.b,c.d", reader.manifest.appOrder)
            assertEquals(listOf("a.b"), reader.manifest.packages)

            val apk = File(temp.root, "restored/app.apk")
            reader.extractApk("a.b", apk)
            assertEquals("apk-bytes", apk.readText())

            val target = File(temp.root, "restored/data")
            reader.extractData("a.b", DataKind.DATA, target)
            assertEquals("level 7", File(target, "save.txt").readText())
            assertEquals("inner", File(target, "sub/inner.txt").readText())
        }
    }

    @Test
    fun leavesOutRegeneratedDirectories() {
        SpaceBackup.Reader(backupWithOneApp()).use { reader ->
            val target = File(temp.root, "restored/data")
            reader.extractData("a.b", DataKind.DATA, target)
            assertFalse(File(target, "cache").exists())
            assertFalse(File(target, "lib").exists())
        }
    }

    @Test
    fun refusesEntriesThatEscapeTheTargetDirectory() {
        val evil = File(temp.root, "evil.zip")
        ZipOutputStream(FileOutputStream(evil)).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write("""{"format":1,"name":"x","appOrder":"","packages":["p"]}""".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("data/p/data/../../../outside.txt"))
            zip.write("pwned".toByteArray())
            zip.closeEntry()
        }
        SpaceBackup.Reader(evil).use { reader ->
            val target = File(temp.root, "restored/data")
            val failure = runCatching { reader.extractData("p", DataKind.DATA, target) }.exceptionOrNull()
            assertTrue(failure is IllegalArgumentException)
            assertFalse(File(temp.root, "restored/outside.txt").exists())
            assertFalse(File(temp.root, "outside.txt").exists())
        }
    }

    @Test
    fun rejectsAFileThatIsNotABackup() {
        val other = File(temp.root, "other.zip")
        ZipOutputStream(FileOutputStream(other)).use { zip ->
            zip.putNextEntry(ZipEntry("readme.txt"))
            zip.write("hi".toByteArray())
            zip.closeEntry()
        }
        val failure = runCatching { SpaceBackup.Reader(other).close() }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }
}
