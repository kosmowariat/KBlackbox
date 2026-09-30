package top.niunaijun.blackboxa.view.health

import android.view.View
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import top.niunaijun.blackboxa.bean.HealthItem
import top.niunaijun.blackboxa.databinding.ItemHealthBinding
import top.niunaijun.blackboxa.util.BindingAdapter

fun healthAdapter() = BindingAdapter(ItemHealthBinding::inflate, HealthItem::title) { binding, item ->
    binding.title.setText(item.title)
    binding.status.text = item.status
    val statusColor = when (item.ok) {
        true -> MaterialR.attr.colorPrimary
        false -> MaterialR.attr.colorError
        null -> MaterialR.attr.colorOnSurfaceVariant
    }
    binding.status.setTextColor(MaterialColors.getColor(binding.root, statusColor))

    val label = item.actionLabel
    val action = item.action
    if (label != null && action != null) {
        binding.action.visibility = View.VISIBLE
        binding.action.setText(label)
        binding.action.setOnClickListener { action() }
    } else {
        binding.action.visibility = View.GONE
        binding.action.setOnClickListener(null)
    }
}
