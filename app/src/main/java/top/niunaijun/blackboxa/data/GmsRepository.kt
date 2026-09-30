package top.niunaijun.blackboxa.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.app.AppManager
import top.niunaijun.blackboxa.bean.GmsBean
import top.niunaijun.blackboxa.bean.GmsInstallBean
import top.niunaijun.blackboxa.util.getString

class GmsRepository {

    suspend fun getGmsInstalledList(): List<GmsBean> = withContext(Dispatchers.IO) {
        val core = BlackBoxCore.get()
        core.users.map {
            val userName =
                    AppManager.mRemarkSharedPreferences.getString(
                            "Remark${it.id}",
                            getString(R.string.default_user_name, it.id.toString())
                    ) ?: ""
            GmsBean(it.id, userName, core.isInstallGms(it.id))
        }
    }

    suspend fun installGms(userID: Int): GmsInstallBean = withContext(Dispatchers.IO) {
        val installResult = BlackBoxCore.get().installGms(userID)
        val message =
                if (installResult.success) {
                    getString(R.string.install_success)
                } else {
                    getString(R.string.install_fail, installResult.msg)
                }
        GmsInstallBean(userID, installResult.success, message)
    }

    suspend fun uninstallGms(userID: Int): GmsInstallBean = withContext(Dispatchers.IO) {
        val core = BlackBoxCore.get()
        val success = core.isInstallGms(userID) && core.uninstallGms(userID)
        val message = if (success) getString(R.string.uninstall_success) else getString(R.string.uninstall_fail)
        GmsInstallBean(userID, success, message)
    }
}
