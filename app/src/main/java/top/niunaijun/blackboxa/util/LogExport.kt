package top.niunaijun.blackboxa.util

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import top.niunaijun.blackboxa.R

/** Collects the logcat lines of this app and its sandboxed apps into a shareable file. */
object LogExport {

    private const val LOG_DIR = "logs"
    private const val LOG_FILE = "apkenclave-log.txt"

    @Throws(IOException::class)
    fun createLogFile(context: Context): File {
        val dir = File(context.cacheDir, LOG_DIR).apply { mkdirs() }
        val file = File(dir, LOG_FILE)
        val process = ProcessBuilder("logcat", "-d", "-v", "threadtime").redirectErrorStream(true).start()
        file.bufferedWriter().use { out ->
            out.write(header(context))
            process.inputStream.bufferedReader().copyTo(out)
        }
        process.waitFor()
        return file
    }

    fun shareIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.about_logs_subject))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(send, context.getString(R.string.about_share_logs))
    }

    private fun header(context: Context): String {
        val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        return "${context.packageName} $version\n" +
                "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), " +
                "${Build.MANUFACTURER} ${Build.MODEL}, ${Build.SUPPORTED_ABIS.joinToString()}\n\n"
    }
}
