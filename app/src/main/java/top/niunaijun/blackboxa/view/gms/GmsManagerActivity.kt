package top.niunaijun.blackboxa.view.gms

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.materialswitch.MaterialSwitch
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.bean.GmsBean
import top.niunaijun.blackboxa.databinding.ActivityGmsBinding
import top.niunaijun.blackboxa.databinding.ItemGmsBinding
import top.niunaijun.blackboxa.util.BindingAdapter
import top.niunaijun.blackboxa.util.InjectionUtil
import top.niunaijun.blackboxa.util.collectStarted
import top.niunaijun.blackboxa.util.inflate
import top.niunaijun.blackboxa.util.showConfirmDialog
import top.niunaijun.blackboxa.util.showInfoDialog
import top.niunaijun.blackboxa.util.toast
import top.niunaijun.blackboxa.view.base.LoadingActivity


class GmsManagerActivity : LoadingActivity() {

    private lateinit var viewModel: GmsViewModel

    private lateinit var mAdapter: BindingAdapter<GmsBean, ItemGmsBinding>

    private val viewBinding: ActivityGmsBinding by inflate()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        initToolbar(viewBinding.toolbarLayout.toolbar, R.string.gms_manager, true)
        initViewModel()

        initRecyclerView()
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this, InjectionUtil.getGmsFactory())[GmsViewModel::class.java]
        showLoading()

        collectStarted(viewModel.users) { users ->
            if (users != null) {
                hideLoading()
                mAdapter.setItems(users)
            }
        }

        collectStarted(viewModel.installResults) { result ->
            val items = mAdapter.getItems()
            val index = items.indexOfFirst { it.userID == result.userID }
            if (index >= 0) {
                val bean = items[index]
                if (result.success) {
                    bean.isInstalledGms = !bean.isInstalledGms
                }
                mAdapter.replaceAt(index, bean)
            }

            hideLoading()

            if (result.success) {
                toast(result.msg)
            } else {
                showInfoDialog(R.string.gms_manager, result.msg)
            }
        }

        viewModel.getInstalledUser()
    }

    private fun initRecyclerView() {
        mAdapter = gmsAdapter()
        mAdapter.onItemClick = { view, item, _ ->
            val checkbox = view.findViewById<MaterialSwitch>(R.id.checkbox)
            if (item.isInstalledGms) {
                uninstallGms(item.userID, checkbox)
            } else {
                installGms(item.userID, checkbox)
            }
        }
        viewBinding.recyclerView.adapter = mAdapter
        viewBinding.recyclerView.layoutManager = LinearLayoutManager(this)

    }

    private fun installGms(userID: Int, checkbox: MaterialSwitch){
        showConfirmDialog(
                R.string.enable_gms,
                getString(R.string.enable_gms_hint),
                onCancel = { checkbox.isChecked = !checkbox.isChecked }
        ) {
            showLoading()
            viewModel.installGms(userID)
        }
    }

    private fun uninstallGms(userID: Int, checkbox: MaterialSwitch){
        showConfirmDialog(
                R.string.disable_gms,
                getString(R.string.disable_gms_hint),
                onCancel = { checkbox.isChecked = !checkbox.isChecked }
        ) {
            showLoading()
            viewModel.uninstallGms(userID)
        }
    }


    companion object{
        fun start(context: Context){
            val intent = Intent(context,GmsManagerActivity::class.java)
            context.startActivity(intent)
        }
    }
}