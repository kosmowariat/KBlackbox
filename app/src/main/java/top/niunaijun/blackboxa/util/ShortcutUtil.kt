package top.niunaijun.blackboxa.util

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.app.App
import top.niunaijun.blackboxa.app.AppManager
import top.niunaijun.blackboxa.bean.AppInfo
import top.niunaijun.blackboxa.databinding.DialogTextInputBinding
import top.niunaijun.blackboxa.util.ContextUtil.openAppSystemSettings
import top.niunaijun.blackboxa.view.main.ShortcutActivity

object ShortcutUtil {

    private const val SHORTCUT_ICON_SIZE = 192

    private fun shortcutId(userID: Int, info: AppInfo) = info.packageName + userID

    private fun launchIntent(context: Context, userID: Int, info: AppInfo) =
            Intent(context, ShortcutActivity::class.java)
                    .setAction(Intent.ACTION_MAIN)
                    .putExtra("pkg", info.packageName)
                    .putExtra("userId", userID)

    private fun buildShortcut(context: Context, userID: Int, info: AppInfo, label: String, icon: Drawable) =
            ShortcutInfoCompat.Builder(context, shortcutId(userID, info))
                    .setIntent(launchIntent(context, userID, info))
                    .setShortLabel(label)
                    .setLongLabel(label)
                    .setIcon(IconCompat.createWithBitmap(icon.toBitmap(SHORTCUT_ICON_SIZE, SHORTCUT_ICON_SIZE)))
                    .build()

    fun createShortcut(context: Context, userID: Int, info: AppInfo) {
        val icon = info.icon
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context) || icon == null) {
            toast(R.string.cannot_create_shortcut)
            return
        }

        val binding = DialogTextInputBinding.inflate(LayoutInflater.from(context))
        binding.inputLayout.hint = context.getString(R.string.shortcut_name)
        binding.input.setText(info.name + userID)

        MaterialAlertDialogBuilder(context)
                .setTitle(R.string.app_shortcut)
                .setView(binding.root)
                .setPositiveButton(R.string.done) { _, _ ->
                    val shortcut = buildShortcut(context, userID, info, binding.input.text.toString(), icon)
                    ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
                    showAllowPermissionDialog(context)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    /** Puts a launched app into the launcher's long-press menu of APKEnclave. */
    fun pushRecentApp(context: Context, userID: Int, info: AppInfo) {
        val icon = info.icon ?: return
        ShortcutManagerCompat.pushDynamicShortcut(context, buildShortcut(context, userID, info, info.name, icon))
    }

    /** Drops the launcher shortcuts of an app that was uninstalled from a space. */
    fun removeShortcuts(context: Context, userID: Int, info: AppInfo) {
        val ids = listOf(shortcutId(userID, info))
        ShortcutManagerCompat.removeDynamicShortcuts(context, ids)
        ShortcutManagerCompat.disableShortcuts(context, ids, context.getString(R.string.shortcut_removed))
    }

    private fun showAllowPermissionDialog(context: Context) {
        if (!AppManager.mBlackBoxLoader.showShortcutPermissionDialog()) {
            return
        }

        MaterialAlertDialogBuilder(context)
                .setTitle(R.string.try_add_shortcut)
                .setMessage(R.string.add_shortcut_fail_msg)
                .setPositiveButton(R.string.done, null)
                .setNegativeButton(R.string.permission_setting) { _, _ ->
                    App.getContext().openAppSystemSettings()
                }
                .setNeutralButton(R.string.no_reminders) { _, _ ->
                    AppManager.mBlackBoxLoader.invalidShortcutPermissionDialog(false)
                }
                .show()
    }
}
