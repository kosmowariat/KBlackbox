package top.niunaijun.blackboxa.util

import android.content.Context
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import top.niunaijun.blackboxa.R

fun Context.showConfirmDialog(
        @StringRes title: Int,
        message: CharSequence,
        onCancel: (() -> Unit)? = null,
        onConfirm: () -> Unit
) {
    MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.done) { _, _ -> onConfirm() }
            .setNegativeButton(R.string.cancel) { _, _ -> onCancel?.invoke() }
            .setOnCancelListener { onCancel?.invoke() }
            .show()
}

fun Context.showInfoDialog(
        @StringRes title: Int,
        message: CharSequence,
        @StringRes buttonText: Int = R.string.done,
        onDismiss: (() -> Unit)? = null
) {
    MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(buttonText) { _, _ -> onDismiss?.invoke() }
            .show()
}
