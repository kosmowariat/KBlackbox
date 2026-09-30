package top.niunaijun.blackboxa.data

import android.content.pm.ApplicationInfo
import android.net.Uri
import android.util.Log
import android.webkit.URLUtil
import androidx.lifecycle.MutableLiveData
import java.io.File
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.core.GmsCore
import top.niunaijun.blackbox.core.env.BEnvironment
import top.niunaijun.blackbox.entity.pm.InstallResult
import top.niunaijun.blackbox.utils.AbiUtils
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.app.AppManager
import top.niunaijun.blackboxa.bean.AppInfo
import top.niunaijun.blackboxa.bean.DuplicateUserBean
import top.niunaijun.blackboxa.bean.InstalledAppBean
import top.niunaijun.blackboxa.bean.UserBean
import top.niunaijun.blackboxa.util.DataDirCopier
import top.niunaijun.blackboxa.util.MemoryManager
import top.niunaijun.blackboxa.util.getString


class AppsRepository {
    companion object {
        const val DEFAULT_USER_ID = 0
        const val USER_PREVIEW_APP_COUNT = 5
    }

    val TAG: String = "AppsRepository"
    private var mInstalledList = mutableListOf<AppInfo>()

    
    private fun safeLoadAppLabel(applicationInfo: ApplicationInfo): String {
        return try {
            BlackBoxCore.getPackageManager().getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load label for ${applicationInfo.packageName}: ${e.message}")
            applicationInfo.packageName 
        }
    }

    
    private fun safeLoadAppIcon(
            applicationInfo: ApplicationInfo
    ): android.graphics.drawable.Drawable? {
        return try {
            
            if (MemoryManager.shouldSkipIconLoading()) {
                Log.w(
                        TAG,
                        "Memory usage high (${MemoryManager.getMemoryUsagePercentage()}%), skipping icon for ${applicationInfo.packageName}"
                )
                return null
            }

            val icon = BlackBoxCore.getPackageManager().getApplicationIcon(applicationInfo)

            
            if (icon is android.graphics.drawable.BitmapDrawable) {
                val bitmap = icon.bitmap
                
                if (bitmap.width > 96 || bitmap.height > 96) {
                    try {
                        val scaledBitmap =
                                android.graphics.Bitmap.createScaledBitmap(bitmap, 96, 96, true)
                        android.graphics.drawable.BitmapDrawable(
                                BlackBoxCore.getPackageManager()
                                        .getResourcesForApplication(applicationInfo.packageName),
                                scaledBitmap
                        )
                    } catch (e: Exception) {
                        Log.w(
                                TAG,
                                "Failed to scale icon for ${applicationInfo.packageName}: ${e.message}"
                        )
                        icon
                    }
                } else {
                    icon
                }
            } else {
                icon
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load icon for ${applicationInfo.packageName}: ${e.message}")
            null 
        }
    }

    fun previewInstallList() {
        try {
            synchronized(mInstalledList) {
                val installedApplications: List<ApplicationInfo> =
                        BlackBoxCore.getPackageManager().getInstalledApplications(0)
                val installedList = mutableListOf<AppInfo>()

                for (installedApplication in installedApplications) {
                    try {
                        val file = File(installedApplication.sourceDir)

                        if ((installedApplication.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
                                continue

                        if (!AbiUtils.isSupport(file)) continue

                        
                        if (BlackBoxCore.get().isBlackBoxApp(installedApplication.packageName)) {
                            Log.d(
                                    TAG,
                                    "Filtering out BlackBox app: ${installedApplication.packageName}"
                            )
                            continue
                        }

                        val isXpModule = false

                        val info =
                                AppInfo(
                                        safeLoadAppLabel(installedApplication),
                                        safeLoadAppIcon(
                                                installedApplication
                                        ), 
                                        installedApplication.packageName,
                                        installedApplication.sourceDir,
                                        isXpModule
                                )
                        installedList.add(info)
                    } catch (e: Exception) {
                        Log.e(
                                TAG,
                                "Error processing app ${installedApplication.packageName}: ${e.message}"
                        )
                    }
                }
                this.mInstalledList.clear()
                this.mInstalledList.addAll(installedList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in previewInstallList: ${e.message}")
        }
    }

    fun getInstalledAppList(
            userID: Int,
            loadingLiveData: MutableLiveData<Boolean>,
            appsLiveData: MutableLiveData<List<InstalledAppBean>>
    ) {
        try {
            loadingLiveData.postValue(true)
            synchronized(mInstalledList) {
                if (mInstalledList.isEmpty()) {
                    previewInstallList()
                }
                val blackBoxCore = BlackBoxCore.get()
                Log.d(TAG, mInstalledList.joinToString(","))
                val newInstalledList =
                        mInstalledList.map {
                            InstalledAppBean(
                                    it.name,
                                    it.icon, 
                                    it.packageName,
                                    it.sourceDir,
                                    blackBoxCore.isInstalled(it.packageName, userID)
                            )
                        }
                appsLiveData.postValue(newInstalledList)
                loadingLiveData.postValue(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getInstalledAppList: ${e.message}")
            loadingLiveData.postValue(false)
            appsLiveData.postValue(emptyList())
        }
    }

    fun getVmInstallList(userId: Int, appsLiveData: MutableLiveData<List<AppInfo>>) {
        try {
            
            if (MemoryManager.isMemoryCritical()) {
                Log.w(
                        TAG,
                        "Memory critical (${MemoryManager.getMemoryUsagePercentage()}%), forcing garbage collection"
                )
                MemoryManager.forceGarbageCollectionIfNeeded()
            }

            val blackBoxCore = BlackBoxCore.get()

            
            val users = blackBoxCore.users
            Log.d(TAG, "getVmInstallList: userId=$userId, total users=${users.size}")
            users.forEach { user -> Log.d(TAG, "User: id=${user.id}, name=${user.name}") }

            val sortListData = AppManager.mRemarkSharedPreferences.getString("AppList$userId", "")
            val sortList = sortListData?.split(",")

            
            var applicationList: List<ApplicationInfo>? = null
            var retryCount = 0
            val maxRetries = 3

            while (applicationList == null && retryCount < maxRetries) {
                try {
                    applicationList = blackBoxCore.getInstalledApplications(0, userId)
                    if (applicationList == null) {
                        Log.w(
                                TAG,
                                "getVmInstallList: Attempt ${retryCount + 1} returned null, retrying..."
                        )
                        retryCount++
                        Thread.sleep(100) 
                    }
                } catch (e: Exception) {
                    Log.e(
                            TAG,
                            "getVmInstallList: Error getting applications on attempt ${retryCount + 1}: ${e.message}"
                    )
                    retryCount++
                    if (retryCount < maxRetries) {
                        Thread.sleep(200) 
                    }
                }
            }

            
            if (applicationList == null) {
                Log.e(
                        TAG,
                        "getVmInstallList: applicationList is null for userId=$userId after $maxRetries attempts"
                )
                appsLiveData.postValue(emptyList())
                return
            }

            
            Log.d(
                    TAG,
                    "getVmInstallList: userId=$userId, applicationList.size=${applicationList.size}"
            )
            if (applicationList.isNotEmpty()) {
                Log.d(TAG, "First app: ${applicationList.first().packageName}")
            } else {
                Log.w(TAG, "getVmInstallList: No applications found for userId=$userId")
            }

            val appInfoList = mutableListOf<AppInfo>()

            
            val sortedApplicationList =
                    if (!sortList.isNullOrEmpty()) {
                        try {
                            applicationList.sortedWith(AppsSortComparator(sortList))
                        } catch (e: Exception) {
                            Log.e(TAG, "getVmInstallList: Error sorting applications: ${e.message}")
                            applicationList 
                        }
                    } else {
                        applicationList
                    }

            
            sortedApplicationList.forEachIndexed { index, applicationInfo ->
                try {
                    
                    if (index > 0 && index % 25 == 0) {
                        if (MemoryManager.isMemoryCritical()) {
                            Log.w(TAG, "Memory critical during processing, forcing GC")
                            MemoryManager.forceGarbageCollectionIfNeeded()
                        }
                    }

                    
                    if (applicationInfo == null) {
                        Log.w(
                                TAG,
                                "getVmInstallList: Skipping null applicationInfo at index $index"
                        )
                        return@forEachIndexed
                    }

                    
                    if (applicationInfo.packageName.isNullOrBlank()) {
                        Log.w(
                                TAG,
                                "getVmInstallList: Skipping app with null/blank package name at index $index"
                        )
                        return@forEachIndexed
                    }

                    val info =
                            AppInfo(
                                    safeLoadAppLabel(applicationInfo),
                                    safeLoadAppIcon(
                                            applicationInfo
                                    ), 
                                    applicationInfo.packageName,
                                    applicationInfo.sourceDir ?: "",
                                    false
                            )

                    appInfoList.add(info)

                    
                    if (index > 0 && index % 50 == 0) {
                        Log.d(
                                TAG,
                                "getVmInstallList: Processed $index/${sortedApplicationList.size} apps - ${MemoryManager.getMemoryInfo()}"
                        )
                    }
                } catch (e: Exception) {
                    Log.e(
                            TAG,
                            "getVmInstallList: Error processing app at index $index (${applicationInfo?.packageName}): ${e.message}"
                    )
                    
                }
            }

            Log.d(
                    TAG,
                    "getVmInstallList: processed ${appInfoList.size} apps - ${MemoryManager.getMemoryInfo()}"
            )

            
            
            if (appInfoList.isEmpty()) {
                Log.d(
                        TAG,
                        "getVmInstallList: No virtual apps found for userId=$userId, showing empty list (correct for new users)"
                )
            } else {
                Log.d(
                        TAG,
                        "getVmInstallList: Showing ${appInfoList.size} virtual apps for userId=$userId"
                )
            }

            
            try {
                appsLiveData.postValue(appInfoList)
            } catch (e: Exception) {
                Log.e(TAG, "getVmInstallList: Error posting to LiveData: ${e.message}")
                
                try {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        try {
                            appsLiveData.postValue(appInfoList)
                        } catch (e2: Exception) {
                            Log.e(
                                    TAG,
                                    "getVmInstallList: Fallback posting also failed: ${e2.message}"
                            )
                        }
                    }
                } catch (e3: Exception) {
                    Log.e(
                            TAG,
                            "getVmInstallList: Could not schedule fallback posting: ${e3.message}"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getVmInstallList: ${e.message}")
            try {
                appsLiveData.postValue(emptyList())
            } catch (e2: Exception) {
                Log.e(TAG, "getVmInstallList: Error posting empty list: ${e2.message}")
            }
        }
    }

    fun installApk(source: String, userId: Int, resultLiveData: MutableLiveData<String>) {
        try {
            
            if (source.contains("blackbox") ||
                            source.contains("niunaijun") ||
                            source.contains("vspace") ||
                            source.contains("virtual")
            ) {
                
                try {
                    val blackBoxCore = BlackBoxCore.get()
                    val hostPackageName = BlackBoxCore.getHostPkg()

                    
                    if (!URLUtil.isValidUrl(source)) {
                        val file = File(source)
                        if (file.exists()) {
                            val packageInfo =
                                    BlackBoxCore.getPackageManager()
                                            .getPackageArchiveInfo(source, 0)
                            if (packageInfo != null && packageInfo.packageName == hostPackageName) {
                                resultLiveData.postValue(
                                        "Cannot install BlackBox app from within BlackBox. This would create infinite recursion and is not allowed for security reasons."
                                )
                                return
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not verify if this is BlackBox app: ${e.message}")
                }
            }

            val blackBoxCore = BlackBoxCore.get()
            val installResult =
                    if (URLUtil.isValidUrl(source)) {
                        val uri = Uri.parse(source)
                        blackBoxCore.installPackageAsUser(uri, userId)
                    } else {
                        blackBoxCore.installPackageAsUser(source, userId)
                    }

            if (installResult.success) {
                updateAppSortList(userId, installResult.packageName, true)
                resultLiveData.postValue(getString(R.string.install_success))
            } else {
                resultLiveData.postValue(getString(R.string.install_fail, installResult.msg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error installing APK: ${e.message}")
            resultLiveData.postValue(getString(R.string.install_error, e.message.orEmpty()))
        }
    }

    fun unInstall(packageName: String, userID: Int, resultLiveData: MutableLiveData<String>) {
        try {
            BlackBoxCore.get().uninstallPackageAsUser(packageName, userID)
            updateAppSortList(userID, packageName, false)
            resultLiveData.postValue(getString(R.string.uninstall_success))
        } catch (e: Exception) {
            Log.e(TAG, "Error uninstalling APK: ${e.message}")
            resultLiveData.postValue(getString(R.string.uninstall_error, e.message.orEmpty()))
        }
    }

    fun launchApk(packageName: String, userId: Int, launchLiveData: MutableLiveData<Boolean>) {
        launchLiveData.postValue(launchApk(packageName, userId))
    }

    fun launchApk(packageName: String, userId: Int): Boolean {
        return try {
            BlackBoxCore.get().launchApk(packageName, userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching APK: ${e.message}")
            false
        }
    }

    fun clearApkData(packageName: String, userID: Int, resultLiveData: MutableLiveData<String>) {
        try {
            BlackBoxCore.get().clearPackage(packageName, userID)
            resultLiveData.postValue(getString(R.string.clear_success))
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing APK data: ${e.message}")
            resultLiveData.postValue(getString(R.string.clear_error, e.message.orEmpty()))
        }
    }

    fun getUserList(usersLiveData: MutableLiveData<List<UserBean>>) {
        try {
            val blackBoxCore = BlackBoxCore.get()
            val userIds = blackBoxCore.users.map { it.id }.ifEmpty { listOf(DEFAULT_USER_ID) }.sorted()
            val users =
                    userIds.map { id ->
                        val apps = sortedInstalledApplications(id).filterNot { GmsCore.isGoogleAppOrService(it.packageName) }
                        UserBean(
                                id,
                                getUserName(id),
                                apps.size,
                                apps.take(USER_PREVIEW_APP_COUNT).map {
                                    AppInfo(safeLoadAppLabel(it), safeLoadAppIcon(it), it.packageName, it.sourceDir, false)
                                }
                        )
                    }
            usersLiveData.postValue(users)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user list", e)
            usersLiveData.postValue(emptyList())
        }
    }

    fun isGmsSupported(): Boolean = BlackBoxCore.get().isSupportGms

    fun createUser(
            name: String,
            installGms: Boolean,
            createdLiveData: MutableLiveData<UserBean?>,
            errorLiveData: MutableLiveData<String?>
    ) {
        try {
            val blackBoxCore = BlackBoxCore.get()
            val nextId = createNamedUser(name)
            if (installGms) {
                val result = blackBoxCore.installGms(nextId)
                if (!result.success) {
                    errorLiveData.postValue(getString(R.string.install_fail, result.msg))
                }
            }
            createdLiveData.postValue(UserBean(nextId, getUserName(nextId), 0, emptyList()))
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user", e)
            errorLiveData.postValue(getString(R.string.create_user_failed))
        }
    }

    fun getDuplicateRequest(user: UserBean): DuplicateUserBean {
        return DuplicateUserBean(user, getUserApps(user.id))
    }

    fun getUserApps(userId: Int): List<AppInfo> {
        return sortedInstalledApplications(userId)
                .sortedBy { GmsCore.isGoogleAppOrService(it.packageName) }
                .map { AppInfo(safeLoadAppLabel(it), safeLoadAppIcon(it), it.packageName, it.sourceDir, false) }
    }

    fun duplicateUser(
            sourceUserId: Int,
            name: String,
            copyDataFor: Set<String>,
            createdLiveData: MutableLiveData<UserBean?>,
            errorLiveData: MutableLiveData<String?>
    ) {
        try {
            val targetUserId = createNamedUser(name)
            val failedApps = mutableListOf<String>()
            sortedInstalledApplications(sourceUserId).forEach { app ->
                val result = installCopyForUser(app, targetUserId)
                if (!result.success) {
                    Log.w(TAG, "Duplicate: install of ${app.packageName} failed: ${result.msg}")
                    failedApps += safeLoadAppLabel(app)
                } else if (app.packageName in copyDataFor) {
                    copyAppData(app.packageName, sourceUserId, targetUserId)
                }
            }
            AppManager.mRemarkSharedPreferences.getString("AppList$sourceUserId", null)?.let {
                AppManager.mRemarkSharedPreferences.edit().putString("AppList$targetUserId", it).apply()
            }
            if (failedApps.isNotEmpty()) {
                errorLiveData.postValue(getString(R.string.duplicate_failed_apps, failedApps.joinToString()))
            }
            createdLiveData.postValue(UserBean(targetUserId, getUserName(targetUserId), 0, emptyList()))
        } catch (e: Exception) {
            Log.e(TAG, "Error duplicating user $sourceUserId", e)
            errorLiveData.postValue(getString(R.string.create_user_failed))
        }
    }

    private fun createNamedUser(name: String): Int {
        val blackBoxCore = BlackBoxCore.get()
        if (blackBoxCore.users.isEmpty()) {
            blackBoxCore.createUser(DEFAULT_USER_ID)
        }
        val nextId = blackBoxCore.users.maxOf { it.id } + 1
        blackBoxCore.createUser(nextId)
        renameUser(nextId, name)
        return nextId
    }

    private fun installCopyForUser(app: ApplicationInfo, userId: Int): InstallResult {
        val blackBoxCore = BlackBoxCore.get()
        val installedApk = File(app.sourceDir)
        if (!installedApk.absolutePath.startsWith(BEnvironment.getAppRootDir().absolutePath)) {
            return blackBoxCore.installPackageAsUser(app.packageName, userId)
        }
        blackBoxCore.users.forEach { blackBoxCore.stopPackage(app.packageName, it.id) }
        val tempApk = File(BEnvironment.getCacheDir(), "duplicate-${app.packageName}.apk")
        return try {
            installedApk.copyTo(tempApk, overwrite = true)
            installedApk.setWritable(true)
            blackBoxCore.installPackageAsUser(tempApk, userId)
        } finally {
            tempApk.delete()
        }
    }

    private fun copyAppData(packageName: String, sourceUserId: Int, targetUserId: Int) {
        BlackBoxCore.get().stopPackage(packageName, sourceUserId)
        listOf(
                BEnvironment.getDataDir(packageName, sourceUserId) to BEnvironment.getDataDir(packageName, targetUserId),
                BEnvironment.getDeDataDir(packageName, sourceUserId) to BEnvironment.getDeDataDir(packageName, targetUserId),
                BEnvironment.getExternalDataDir(packageName, sourceUserId) to BEnvironment.getExternalDataDir(packageName, targetUserId)
        ).forEach { (source, target) ->
            try {
                DataDirCopier.copy(source, target)
            } catch (e: Exception) {
                Log.e(TAG, "Duplicate: copying ${source.path} failed", e)
            }
        }
    }

    fun renameUser(userId: Int, name: String) {
        AppManager.mRemarkSharedPreferences.edit().apply {
            if (name.isBlank()) remove("Remark$userId") else putString("Remark$userId", name.trim())
            apply()
        }
    }

    fun deleteUser(userId: Int) {
        try {
            BlackBoxCore.get().deleteUser(userId)
            AppManager.mRemarkSharedPreferences.edit().apply {
                remove("Remark$userId")
                remove("AppList$userId")
                apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user $userId", e)
        }
    }

    fun getUserName(userId: Int): String {
        val remark = AppManager.mRemarkSharedPreferences.getString("Remark$userId", null)
        return if (remark.isNullOrBlank()) getString(R.string.default_user_name, userId.toString()) else remark
    }

    private fun sortedInstalledApplications(userId: Int): List<ApplicationInfo> {
        val applications = BlackBoxCore.get().getInstalledApplications(0, userId)
        val sortList = AppManager.mRemarkSharedPreferences.getString("AppList$userId", "")
                ?.split(",")
                ?.filter { it.isNotEmpty() }
        return if (sortList.isNullOrEmpty()) applications else applications.sortedWith(AppsSortComparator(sortList))
    }

    
    private fun updateAppSortList(userID: Int, pkg: String, isAdd: Boolean) {
        try {
            val savedSortList = AppManager.mRemarkSharedPreferences.getString("AppList$userID", "")

            val sortList = linkedSetOf<String>()
            if (savedSortList != null) {
                sortList.addAll(savedSortList.split(","))
            }

            if (isAdd) {
                sortList.add(pkg)
            } else {
                sortList.remove(pkg)
            }

            AppManager.mRemarkSharedPreferences.edit().apply {
                putString("AppList$userID", sortList.joinToString(","))
                apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating app sort list: ${e.message}")
        }
    }

    
    fun updateApkOrder(userID: Int, dataList: List<AppInfo>) {
        try {
            AppManager.mRemarkSharedPreferences.edit().apply {
                putString("AppList$userID", dataList.joinToString(",") { it.packageName })
                apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating APK order: ${e.message}")
        }
    }
}
