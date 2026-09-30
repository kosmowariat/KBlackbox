package top.niunaijun.blackboxa.view.apps

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import top.niunaijun.blackboxa.bean.AppInfo
import top.niunaijun.blackboxa.databinding.ItemAppBinding
import top.niunaijun.blackboxa.util.BindingAdapter

private const val MAX_ICON_SIZE = 96

fun appsAdapter() = BindingAdapter(ItemAppBinding::inflate, AppInfo::packageName) { binding, item ->
    binding.name.text = item.name
    binding.icon.setImageDrawable(item.icon?.let { downscale(binding.root, it) } ?: placeholder(binding.root))
}

private fun placeholder(view: View): Drawable =
        ColorDrawable(MaterialColors.getColor(view, MaterialR.attr.colorSurfaceContainerHighest))

/** Keeps large bitmap icons from filling memory in a grid of many apps. */
private fun downscale(view: View, icon: Drawable): Drawable {
    if (icon !is BitmapDrawable) {
        return icon
    }
    val bitmap = icon.bitmap
    if (bitmap.width <= MAX_ICON_SIZE && bitmap.height <= MAX_ICON_SIZE) {
        return icon
    }
    return BitmapDrawable(view.resources, Bitmap.createScaledBitmap(bitmap, MAX_ICON_SIZE, MAX_ICON_SIZE, true))
}
