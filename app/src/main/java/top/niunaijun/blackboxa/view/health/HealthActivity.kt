package top.niunaijun.blackboxa.view.health

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.bean.HealthItem
import top.niunaijun.blackboxa.databinding.ActivityHealthBinding
import top.niunaijun.blackboxa.util.StoragePermission
import top.niunaijun.blackboxa.util.closeApp
import top.niunaijun.blackboxa.util.inflate
import top.niunaijun.blackboxa.util.toast
import top.niunaijun.blackboxa.view.base.BaseActivity

class HealthActivity : BaseActivity() {

    private val viewBinding: ActivityHealthBinding by inflate()

    private val mAdapter = healthAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        initToolbar(viewBinding.toolbarLayout.toolbar, R.string.health_title, true)
        viewBinding.recyclerView.layoutManager = LinearLayoutManager(this)
        viewBinding.recyclerView.adapter = mAdapter
        viewBinding.closeApp.setOnClickListener { closeApp() }
    }

    override fun onResume() {
        super.onResume()
        mAdapter.setItems(buildItems())
    }

    private fun buildItems(): List<HealthItem> {
        return listOf(storageItem(), batteryItem(), notificationsItem(), shortcutsItem(), engineItem())
    }

    private fun storageItem(): HealthItem {
        val granted = StoragePermission.isGranted(this)
        return HealthItem(
                R.string.health_storage,
                getString(if (granted) R.string.health_granted else R.string.health_not_granted),
                granted,
                if (granted) null else R.string.health_open_settings,
                if (granted) null else { { openSettings(StoragePermission.settingsIntent(this)) } }
        )
    }

    private fun batteryItem(): HealthItem {
        val unrestricted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                (getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)
        return HealthItem(
                R.string.health_battery,
                getString(if (unrestricted) R.string.health_battery_ok else R.string.health_battery_limited),
                unrestricted,
                if (unrestricted) null else R.string.health_open_settings,
                if (unrestricted) null else {
                    { openSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                }
        )
    }

    private fun notificationsItem(): HealthItem {
        val enabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
        return HealthItem(
                R.string.health_notifications,
                getString(if (enabled) R.string.health_enabled else R.string.health_disabled),
                enabled,
                if (enabled) null else R.string.health_open_settings,
                if (enabled) null else {
                    {
                        openSettings(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                        )
                    }
                }
        )
    }

    private fun shortcutsItem(): HealthItem {
        val supported = ShortcutManagerCompat.isRequestPinShortcutSupported(this)
        return HealthItem(
                R.string.health_shortcuts,
                getString(if (supported) R.string.health_supported else R.string.health_not_supported),
                supported
        )
    }

    private fun engineItem(): HealthItem {
        val abi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty()
        return HealthItem(
                R.string.health_engine,
                getString(R.string.health_engine_info, Build.VERSION.RELEASE, abi, packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()),
                null
        )
    }

    private fun openSettings(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(R.string.health_unavailable)
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, HealthActivity::class.java))
        }
    }
}
