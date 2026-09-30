package top.niunaijun.blackboxa.view.gms

import top.niunaijun.blackboxa.bean.GmsBean
import top.niunaijun.blackboxa.databinding.ItemGmsBinding
import top.niunaijun.blackboxa.util.BindingAdapter

fun gmsAdapter() =
        BindingAdapter(ItemGmsBinding::inflate, GmsBean::userID) { binding, item ->
            binding.tvTitle.text = item.userName
            binding.checkbox.isChecked = item.isInstalledGms
            binding.checkbox.setOnCheckedChangeListener { buttonView, _ ->
                if (buttonView.isPressed) {
                    binding.root.performClick()
                }
            }
        }
