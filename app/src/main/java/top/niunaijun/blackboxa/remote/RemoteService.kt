package top.niunaijun.blackboxa.remote

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.SecureRandom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.util.InjectionUtil
import top.niunaijun.blackboxa.util.toast

/** Keeps the [RemoteServer] alive in the background while the web panel is switched on. */
class RemoteService : Service() {

    private var server: RemoteServer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (server != null) return START_NOT_STICKY
        startForeground(NOTIFICATION_ID, buildNotification())

        val address = localAddress()
        if (address == null) {
            Log.w(TAG, "No local network address, the web panel cannot be reached")
            toast(R.string.remote_panel_no_network)
            stopSelf()
            return START_NOT_STICKY
        }

        val token = newToken()
        val remoteServer = RemoteServer(InjectionUtil.appsRepository, assets, token, PORT)
        try {
            remoteServer.start()
        } catch (e: IOException) {
            Log.e(TAG, "Error starting the web panel on port $PORT", e)
            toast(R.string.remote_panel_start_failed)
            stopSelf()
            return START_NOT_STICKY
        }
        server = remoteServer
        mutableUrl.value = "http://$address:$PORT/?t=$token"
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        server?.stop()
        server = null
        mutableUrl.value = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    manager.createNotificationChannel(
                            NotificationChannel(
                                    CHANNEL_ID,
                                    getString(R.string.remote_panel_title),
                                    NotificationManager.IMPORTANCE_LOW
                            )
                    )
                    Notification.Builder(this, CHANNEL_ID)
                } else {
                    @Suppress("DEPRECATION") Notification.Builder(this)
                }
        return builder.setSmallIcon(R.drawable.ic_pref_smartphone)
                .setContentTitle(getString(R.string.remote_panel_title))
                .setContentText(getString(R.string.remote_panel_notification))
                .setOngoing(true)
                .build()
    }

    private fun newToken(): String {
        val random = SecureRandom()
        return String(CharArray(TOKEN_LENGTH) { TOKEN_ALPHABET[random.nextInt(TOKEN_ALPHABET.length)] })
    }

    private fun localAddress(): String? =
            NetworkInterface.getNetworkInterfaces().toList()
                    .filter { it.isUp && !it.isLoopback }
                    .flatMap { it.inetAddresses.toList() }
                    .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }
                    ?.hostAddress

    companion object {
        private const val TAG = "RemoteService"
        private const val PORT = 8765
        private const val NOTIFICATION_ID = 8765
        private const val CHANNEL_ID = "remote_panel"
        private const val TOKEN_LENGTH = 10
        private const val TOKEN_ALPHABET = "abcdefghijkmnpqrstuvwxyz23456789"

        private val mutableUrl = MutableStateFlow<String?>(null)

        /** Address of the running web panel including the access token, or null when it is off. */
        val url: StateFlow<String?> = mutableUrl.asStateFlow()

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, RemoteService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RemoteService::class.java))
        }
    }
}
