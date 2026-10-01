package top.niunaijun.blackboxa.util

import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject

/** One app to put into a backup: its APK and the data directories to save, keyed by [DataKind]. */
class BackupApp(val packageName: String, val apk: File, val dataDirs: Map<DataKind, File>)

enum class DataKind(val folder: String) { DATA("data"), DEVICE_PROTECTED("de"), EXTERNAL("ext") }

class BackupManifest(val name: String, val appOrder: String, val packages: List<String>)

/** Reads and writes the zip layout of a space backup: manifest.json, apps/<pkg>.apk and data/<pkg>/<kind>/... */
object SpaceBackup {

    private const val MANIFEST_ENTRY = "manifest.json"
    private const val FORMAT_VERSION = 1
    private const val BUFFER_SIZE = 64 * 1024

    fun write(out: OutputStream, name: String, appOrder: String, apps: List<BackupApp>) {
        ZipOutputStream(out.buffered(BUFFER_SIZE)).use { zip ->
            val manifest = JSONObject()
                    .put("format", FORMAT_VERSION)
                    .put("name", name)
                    .put("appOrder", appOrder)
                    .put("packages", JSONArray(apps.map { it.packageName }))
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(manifest.toString().toByteArray())
            zip.closeEntry()

            apps.forEach { app ->
                zip.putNextEntry(ZipEntry("apps/${app.packageName}.apk"))
                app.apk.inputStream().use { it.copyTo(zip, BUFFER_SIZE) }
                zip.closeEntry()
                app.dataDirs.forEach { (kind, dir) ->
                    addDirectory(zip, "data/${app.packageName}/${kind.folder}", dir)
                }
            }
        }
    }

    private fun addDirectory(zip: ZipOutputStream, prefix: String, root: File) {
        val top = root.listFiles() ?: return
        top.filter { it.name !in DataDirCopier.SKIPPED_DIRS && !DataDirCopier.isSymbolicLink(it) }
                .forEach { addEntry(zip, "$prefix/${it.name}", it) }
    }

    private fun addEntry(zip: ZipOutputStream, name: String, file: File) {
        if (file.isDirectory) {
            zip.putNextEntry(ZipEntry("$name/"))
            zip.closeEntry()
            file.listFiles()
                    ?.filterNot { DataDirCopier.isSymbolicLink(it) }
                    ?.forEach { addEntry(zip, "$name/${it.name}", it) }
        } else if (file.isFile) {
            zip.putNextEntry(ZipEntry(name))
            file.inputStream().use { it.copyTo(zip, BUFFER_SIZE) }
            zip.closeEntry()
        }
    }

    /** An opened backup file; entries are read lazily and every extraction stays inside its target. */
    class Reader(private val file: File) : Closeable {

        private val zip = ZipFile(file)

        val manifest: BackupManifest = readManifest()

        private fun readManifest(): BackupManifest {
            val entry = requireNotNull(zip.getEntry(MANIFEST_ENTRY)) { "Missing $MANIFEST_ENTRY" }
            val json = JSONObject(zip.getInputStream(entry).use(InputStream::readBytes).decodeToString())
            require(json.optInt("format") == FORMAT_VERSION) { "Unsupported backup format" }
            val packages = json.getJSONArray("packages")
            return BackupManifest(
                    json.optString("name"),
                    json.optString("appOrder"),
                    List(packages.length()) { packages.getString(it) }
            )
        }

        fun extractApk(packageName: String, target: File) {
            val entry = requireNotNull(zip.getEntry("apps/$packageName.apk")) { "Missing APK of $packageName" }
            target.parentFile?.mkdirs()
            zip.getInputStream(entry).use { input -> target.outputStream().use { input.copyTo(it, BUFFER_SIZE) } }
        }

        fun extractData(packageName: String, kind: DataKind, target: File) {
            val prefix = "data/$packageName/${kind.folder}/"
            val canonicalTarget = target.canonicalFile
            zip.entries().asSequence().filter { it.name.startsWith(prefix) }.forEach { entry ->
                val destination = File(canonicalTarget, entry.name.removePrefix(prefix)).canonicalFile
                require(destination.path.startsWith(canonicalTarget.path + File.separator)) {
                    "Unsafe entry ${entry.name}"
                }
                if (entry.isDirectory) {
                    destination.mkdirs()
                } else {
                    destination.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        destination.outputStream().use { input.copyTo(it, BUFFER_SIZE) }
                    }
                }
            }
        }

        override fun close() = zip.close()
    }
}
