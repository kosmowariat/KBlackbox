package top.niunaijun.blackboxa.view.list

import android.view.View
import top.niunaijun.blackboxa.bean.InstalledAppBean
import top.niunaijun.blackboxa.databinding.ItemPackageBinding
import top.niunaijun.blackboxa.util.BindingAdapter

fun installedAppsAdapter() =
        BindingAdapter(ItemPackageBinding::inflate, InstalledAppBean::packageName) { binding, item ->
            binding.icon.setImageDrawable(item.icon)
            binding.name.text = item.name
            binding.packageName.text = item.packageName
            binding.installedLabel.visibility = if (item.isInstall) View.VISIBLE else View.GONE
        }
