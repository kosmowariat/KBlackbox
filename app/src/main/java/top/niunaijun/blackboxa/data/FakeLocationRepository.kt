package top.niunaijun.blackboxa.data

import android.content.pm.ApplicationInfo
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.entity.location.BLocation
import top.niunaijun.blackbox.fake.frameworks.BLocationManager
import top.niunaijun.blackboxa.bean.FakeLocationBean


class FakeLocationRepository {
    val TAG: String = "FakeLocationRepository"

    suspend fun setPattern(userId: Int, pkg: String, pattern: Int) = withContext(Dispatchers.IO) {
        BLocationManager.get().setPattern(userId, pkg, pattern)
    }

    private fun getPattern(userId: Int, pkg: String): Int {
        return BLocationManager.get().getPattern(userId, pkg)
    }

    private fun getLocation(userId: Int, pkg: String): BLocation? {
        return BLocationManager.get().getLocation(userId, pkg)
    }

    suspend fun setLocation(userId: Int, pkg: String, location: BLocation) = withContext(Dispatchers.IO) {
        BLocationManager.get().setLocation(userId, pkg, location)
    }

    suspend fun getInstalledAppList(userID: Int): List<FakeLocationBean> = withContext(Dispatchers.IO) {
        val packageManager = BlackBoxCore.getPackageManager()
        val installedApplications: List<ApplicationInfo> = BlackBoxCore.get().getInstalledApplications(0, userID)
        installedApplications.map {
            FakeLocationBean(
                    userID,
                    it.loadLabel(packageManager).toString(),
                    it.loadIcon(packageManager),
                    it.packageName,
                    getPattern(userID, it.packageName),
                    getLocation(userID, it.packageName)
            )
        }
    }
}
