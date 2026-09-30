package top.niunaijun.blackboxa.view.fake

import top.niunaijun.blackbox.fake.frameworks.BLocationManager
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.bean.FakeLocationBean
import top.niunaijun.blackboxa.databinding.ItemFakeBinding
import top.niunaijun.blackboxa.util.BindingAdapter
import top.niunaijun.blackboxa.util.getString

fun fakeLocationAdapter() =
        BindingAdapter(ItemFakeBinding::inflate, FakeLocationBean::packageName) { binding, item ->
            binding.icon.setImageDrawable(item.icon)
            binding.name.text = item.name
            val location = item.fakeLocation
            binding.fakeLocation.text =
                    if (location == null || item.fakeLocationPattern == BLocationManager.CLOSE_MODE) {
                        getString(R.string.real_location)
                    } else {
                        String.format("%f, %f", location.latitude, location.longitude)
                    }
        }
