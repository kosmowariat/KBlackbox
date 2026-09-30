package top.niunaijun.blackboxa.util

import android.content.Context
import android.content.Intent
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

    fun createShortcut(context: Context, userID: Int, info: AppInfo) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            toast(R.string.cannot_create_shortcut)
            return
        }

        val intent = Intent(context, ShortcutActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .putExtra("pkg", info.packageName)
                .putExtra("userId", userID)
        val binding = DialogTextInputBinding.inflate(LayoutInflater.from(context))
        binding.inputLayout.hint = context.getString(R.string.shortcut_name)
        binding.input.setText(info.name + userID)

        MaterialAlertDialogBuilder(context)
                .setTitle(R.string.app_shortcut)
                .setView(binding.root)
                .setPositiveButton(R.string.done) { _, _ ->
                    val label = binding.input.text.toString()
                    val shortcutInfo = ShortcutInfoCompat.Builder(context, info.packageName + userID)
                            .setIntent(intent)
                            .setShortLabel(label)
                            .setLongLabel(label)
                            .setIcon(IconCompat.createWithBitmap(info.icon!!.toBitmap()))
                            .build()
                    ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
                    showAllowPermissionDialog(context)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
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
