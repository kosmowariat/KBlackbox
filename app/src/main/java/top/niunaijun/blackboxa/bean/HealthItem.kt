package top.niunaijun.blackboxa.bean

import androidx.annotation.StringRes

/** One row of the permissions and health screen; [ok] is null for purely informational rows. */
data class HealthItem(
        @StringRes val title: Int,
        val status: String,
        val ok: Boolean?,
        @StringRes val actionLabel: Int? = null,
        val action: (() -> Unit)? = null
)
