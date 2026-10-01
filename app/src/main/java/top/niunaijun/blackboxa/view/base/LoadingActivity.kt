package top.niunaijun.blackboxa.view.base

import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import top.niunaijun.blackboxa.R


abstract class LoadingActivity : BaseActivity() {

    private var loadingDialog: AlertDialog? = null


    fun showLoading() {
        if (loadingDialog?.isShowing == true || isFinishing) {
            return
        }
        loadingDialog = MaterialAlertDialogBuilder(this)
                .setView(R.layout.dialog_loading)
                .setCancelable(false)
                .show()
    }


    fun hideLoading() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    override fun onDestroy() {
        hideLoading()
        super.onDestroy()
    }
}
