package top.niunaijun.blackboxa.data

import android.content.pm.ApplicationInfo
import android.net.Uri
import android.util.Log
import android.webkit.URLUtil
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.core.GmsCore
import top.niunaijun.blackbox.core.env.BEnvironment
import top.niunaijun.blackbox.entity.pm.InstallResult
import top.niunaijun.blackbox.utils.AbiUtils
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.app.AppManager
import top.niunaijun.blackboxa.bean.AppDetails
import top.niunaijun.blackboxa.bean.AppInfo
import top.niunaijun.blackboxa.bean.DuplicateUserBean
import top.niunaijun.blackboxa.bean.InstalledAppBean
import top.niunaijun.blackboxa.bean.UserBean
import top.niunaijun.blackboxa.bean.UserCreationResult
import top.niunaijun.blackboxa.util.BackupApp
import top.niunaijun.blackboxa.util.DataDirCopier
import top.niunaijun.blackboxa.util.DataKind
import top.niunaijun.blackboxa.util.MemoryManager
import top.niunaijun.blackboxa.util.SpaceBackup
import top.niunaijun.blackboxa.util.getString


class AppsRepository {
    companion object {
        const val DEFAULT_USER_ID = 0
        const val USER_PREVIEW_APP_COUNT = 5
        private const val MAX_LOAD_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 150L
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

    suspend fun previewInstallList() = withContext(Dispatchers.IO) { refreshInstalledList() }

    private fun refreshInstalledList() {
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

                        val info =
                                AppInfo(
                                        safeLoadAppLabel(installedApplication),
                                        safeLoadAppIcon(
                                                installedApplication
                                        ), 
                                        installedApplication.packageName,
                                        installedApplication.sourceDir
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

    suspend fun getInstalledAppList(userID: Int): List<InstalledAppBean> = withContext(Dispatchers.IO) {
        try {
            synchronized(mInstalledList) {
                if (mInstalledList.isEmpty()) {
                    refreshInstalledList()
                }
                val blackBoxCore = BlackBoxCore.get()
                mInstalledList.map {
                    InstalledAppBean(
                            it.name,
                            it.icon,
                            it.packageName,
                            it.sourceDir,
                            blackBoxCore.isInstalled(it.packageName, userID)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getInstalledAppList", e)
            emptyList()
        }
    }

    suspend fun getVmInstallList(userId: Int): List<AppInfo> = withContext(Dispatchers.IO) {
        try {
            loadVmInstallList(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error in getVmInstallList", e)
            emptyList()
        }
    }

    private fun loadVmInstallList(userId: Int): List<AppInfo> {
        MemoryManager.forceGarbageCollectionIfNeeded()

        val sortList = AppManager.mRemarkSharedPreferences.getString("AppList$userId", "")?.split(",")
        val applications = loadApplicationsWithRetry(userId)
        if (applications == null) {
            Log.e(TAG, "getVmInstallList: no application list for userId=$userId after $MAX_LOAD_ATTEMPTS attempts")
            return emptyList()
        }

        val sorted =
                if (sortList.isNullOrEmpty()) {
                    applications
                } else {
                    runCatching { applications.sortedWith(AppsSortComparator(sortList)) }.getOrDefault(applications)
                }

        val result = mutableListOf<AppInfo>()
        sorted.forEachIndexed { index, applicationInfo ->
            if (index > 0 && index % 25 == 0) {
                MemoryManager.forceGarbageCollectionIfNeeded()
            }
            if (applicationInfo == null || applicationInfo.packageName.isNullOrBlank()) {
                Log.w(TAG, "getVmInstallList: skipping invalid application entry at index $index")
                return@forEachIndexed
            }
            try {
                result.add(
                        AppInfo(
                                safeLoadAppLabel(applicationInfo),
                                safeLoadAppIcon(applicationInfo),
                                applicationInfo.packageName,
                                applicationInfo.sourceDir ?: ""
                        )
                )
            } catch (e: Exception) {
                Log.e(TAG, "getVmInstallList: error processing ${applicationInfo.packageName}", e)
            }
        }
        Log.d(TAG, "getVmInstallList: userId=$userId, ${result.size} apps - ${MemoryManager.getMemoryInfo()}")
        return result
    }

    private fun loadApplicationsWithRetry(userId: Int): List<ApplicationInfo>? {
        repeat(MAX_LOAD_ATTEMPTS) { attempt ->
            try {
                BlackBoxCore.get().getInstalledApplications(0, userId)?.let { return it }
                Log.w(TAG, "getInstalledApplications returned null for userId=$userId (attempt ${attempt + 1})")
            } catch (e: Exception) {
                Log.e(TAG, "getInstalledApplications failed for userId=$userId (attempt ${attempt + 1})", e)
            }
            Thread.sleep(RETRY_DELAY_MS)
        }
        return null
    }

    suspend fun installApk(source: String, userId: Int): String = withContext(Dispatchers.IO) {
        try {
            if (isHostApk(source)) {
                return@withContext getString(R.string.install_self_blocked)
            }

            val blackBoxCore = BlackBoxCore.get()
            val installResult =
                    if (URLUtil.isValidUrl(source)) {
                        blackBoxCore.installPackageAsUser(Uri.parse(source), userId)
                    } else {
                        blackBoxCore.installPackageAsUser(source, userId)
                    }

            if (installResult.success) {
                updateAppSortList(userId, installResult.packageName, true)
                getString(R.string.install_success)
            } else {
                getString(R.string.install_fail, installResult.msg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error installing APK", e)
            getString(R.string.install_error, e.message.orEmpty())
        }
    }

    /** True when [source] is a local APK of this very app, which must never be installed into itself. */
    private fun isHostApk(source: String): Boolean {
        if (URLUtil.isValidUrl(source) || !File(source).exists()) {
            return false
        }
        return try {
            val archiveInfo = BlackBoxCore.getPackageManager().getPackageArchiveInfo(source, 0)
            archiveInfo != null && archiveInfo.packageName == BlackBoxCore.getHostPkg()
        } catch (e: Exception) {
            Log.w(TAG, "Could not verify whether $source is this app", e)
            false
        }
    }

    suspend fun unInstall(packageName: String, userID: Int): String = withContext(Dispatchers.IO) {
        try {
            BlackBoxCore.get().uninstallPackageAsUser(packageName, userID)
            updateAppSortList(userID, packageName, false)
            getString(R.string.uninstall_success)
        } catch (e: Exception) {
            Log.e(TAG, "Error uninstalling APK", e)
            getString(R.string.uninstall_error, e.message.orEmpty())
        }
    }

    suspend fun launchApk(packageName: String, userId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            BlackBoxCore.get().launchApk(packageName, userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching APK", e)
            false
        }
    }

    suspend fun getRunningPackages(userId: Int, packages: List<String>): Set<String> = withContext(Dispatchers.IO) {
        packages.filter { BlackBoxCore.isRunningApplication(it, userId) }.toSet()
    }

    suspend fun stopAll(userId: Int, packages: List<String>) = withContext(Dispatchers.IO) {
        val core = BlackBoxCore.get()
        packages.forEach {
            try {
                core.stopPackage(it, userId)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping $it", e)
            }
        }
    }

    suspend fun clearApkData(packageName: String, userID: Int): String = withContext(Dispatchers.IO) {
        try {
            BlackBoxCore.get().clearPackage(packageName, userID)
            getString(R.string.clear_success)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing APK data", e)
            getString(R.string.clear_error, e.message.orEmpty())
        }
    }

    suspend fun getUserList(): List<UserBean> = withContext(Dispatchers.IO) {
        try {
            val userIds = BlackBoxCore.get().users.map { it.id }.ifEmpty { listOf(DEFAULT_USER_ID) }.sorted()
            userIds.map { id ->
                val apps = sortedInstalledApplications(id).filterNot { GmsCore.isGoogleAppOrService(it.packageName) }
                UserBean(
                        id,
                        getUserName(id),
                        apps.size,
                        apps.take(USER_PREVIEW_APP_COUNT).map {
                            AppInfo(safeLoadAppLabel(it), safeLoadAppIcon(it), it.packageName, it.sourceDir)
                        }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user list", e)
            emptyList()
        }
    }

    fun isGmsSupported(): Boolean = BlackBoxCore.get().isSupportGms

    suspend fun createUser(name: String, installGms: Boolean): UserCreationResult = withContext(Dispatchers.IO) {
        try {
            val nextId = createNamedUser(name)
            val gmsError =
                    if (installGms) {
                        BlackBoxCore.get().installGms(nextId).takeUnless { it.success }
                                ?.let { getString(R.string.install_fail, it.msg) }
                    } else {
                        null
                    }
            UserCreationResult(UserBean(nextId, getUserName(nextId), 0, emptyList()), gmsError)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user", e)
            UserCreationResult(null, getString(R.string.create_user_failed))
        }
    }

    suspend fun getDuplicateRequest(user: UserBean): DuplicateUserBean = withContext(Dispatchers.IO) {
        DuplicateUserBean(user, getUserApps(user.id))
    }

    private fun getUserApps(userId: Int): List<AppInfo> {
        return sortedInstalledApplications(userId)
                .sortedBy { GmsCore.isGoogleAppOrService(it.packageName) }
                .map { AppInfo(safeLoadAppLabel(it), safeLoadAppIcon(it), it.packageName, it.sourceDir) }
    }

    suspend fun duplicateUser(
            sourceUserId: Int,
            name: String,
            copyDataFor: Set<String>
    ): UserCreationResult = withContext(Dispatchers.IO) {
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
            val error =
                    if (failedApps.isEmpty()) null
                    else getString(R.string.duplicate_failed_apps, failedApps.joinToString())
            UserCreationResult(UserBean(targetUserId, getUserName(targetUserId), 0, emptyList()), error)
        } catch (e: Exception) {
            Log.e(TAG, "Error duplicating user $sourceUserId", e)
            UserCreationResult(null, getString(R.string.create_user_failed))
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

    private fun dataDir(packageName: String, userId: Int, kind: DataKind): File = when (kind) {
        DataKind.DATA -> BEnvironment.getDataDir(packageName, userId)
        DataKind.DEVICE_PROTECTED -> BEnvironment.getDeDataDir(packageName, userId)
        DataKind.EXTERNAL -> BEnvironment.getExternalDataDir(packageName, userId)
    }

    suspend fun getAppDetails(info: AppInfo, userId: Int): AppDetails = withContext(Dispatchers.IO) {
        val packageInfo = BlackBoxCore.getBPackageManager().getPackageInfo(info.packageName, 0, userId)
        val dataBytes = DataKind.entries.sumOf { kind ->
            dataDir(info.packageName, userId, kind).walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
        AppDetails(
                info.name,
                info.packageName,
                packageInfo?.versionName.orEmpty(),
                packageInfo?.let { PackageInfoCompat.getLongVersionCode(it) } ?: 0L,
                dataBytes
        )
    }

    suspend fun getCopyTargets(sourceUserId: Int): List<UserBean> = getUserList().filter { it.id != sourceUserId }

    /** Installs [info] into another space, optionally with its data; returns the message to show. */
    suspend fun copyApp(info: AppInfo, sourceUserId: Int, target: UserBean, withData: Boolean): String =
            withContext(Dispatchers.IO) {
                try {
                    val core = BlackBoxCore.get()
                    if (core.getInstalledApplications(0, target.id).any { it.packageName == info.packageName }) {
                        return@withContext getString(R.string.app_copy_already_there, info.name, target.name)
                    }
                    val app = core.getInstalledApplications(0, sourceUserId).first { it.packageName == info.packageName }
                    val result = installCopyForUser(app, target.id)
                    if (!result.success) {
                        Log.w(TAG, "Copy: install of ${info.packageName} failed: ${result.msg}")
                        return@withContext getString(R.string.app_copy_failed, info.name)
                    }
                    if (withData) {
                        copyAppData(info.packageName, sourceUserId, target.id)
                    }
                    updateAppSortList(target.id, info.packageName, true)
                    getString(R.string.app_copied, info.name, target.name)
                } catch (e: Exception) {
                    Log.e(TAG, "Error copying ${info.packageName} to user ${target.id}", e)
                    getString(R.string.app_copy_failed, info.name)
                }
            }

    /** Writes the apps of a space and their data into [target]; returns an error message or null. */
    suspend fun exportUser(userId: Int, target: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val core = BlackBoxCore.get()
            val apps = sortedInstalledApplications(userId).filterNot { GmsCore.isGoogleAppOrService(it.packageName) }
            apps.forEach { core.stopPackage(it.packageName, userId) }
            val backupApps = apps.map { app ->
                BackupApp(
                        app.packageName,
                        File(app.sourceDir),
                        DataKind.entries.associateWith { dataDir(app.packageName, userId, it) }
                )
            }
            val appOrder = AppManager.mRemarkSharedPreferences.getString("AppList$userId", "").orEmpty()
            val output = requireNotNull(BlackBoxCore.getContext().contentResolver.openOutputStream(target)) {
                "Cannot open $target"
            }
            output.use { SpaceBackup.write(it, getUserName(userId), appOrder, backupApps) }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting user $userId", e)
            getString(R.string.backup_export_failed)
        }
    }

    /** Creates a new space from a backup file, installing its apps and restoring their data. */
    suspend fun importUser(source: Uri): UserCreationResult = withContext(Dispatchers.IO) {
        val backupFile = File(BEnvironment.getCacheDir(), "import-backup.zip")
        try {
            val input = requireNotNull(BlackBoxCore.getContext().contentResolver.openInputStream(source)) {
                "Cannot open $source"
            }
            input.use { stream -> backupFile.outputStream().use { stream.copyTo(it) } }
            SpaceBackup.Reader(backupFile).use { reader ->
                val manifest = reader.manifest
                val userId = createNamedUser(manifest.name)
                val failedApps = manifest.packages.filterNot { restoreApp(reader, it, userId) }
                if (manifest.appOrder.isNotEmpty()) {
                    AppManager.mRemarkSharedPreferences.edit().putString("AppList$userId", manifest.appOrder).apply()
                }
                val error =
                        if (failedApps.isEmpty()) null
                        else getString(R.string.duplicate_failed_apps, failedApps.joinToString())
                UserCreationResult(UserBean(userId, getUserName(userId), 0, emptyList()), error)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error importing backup", e)
            UserCreationResult(null, getString(R.string.backup_import_failed))
        } finally {
            backupFile.delete()
        }
    }

    private fun restoreApp(reader: SpaceBackup.Reader, packageName: String, userId: Int): Boolean {
        val apk = File(BEnvironment.getCacheDir(), "import-$packageName.apk")
        return try {
            reader.extractApk(packageName, apk)
            val result = BlackBoxCore.get().installPackageAsUser(apk, userId)
            if (result.success) {
                DataKind.entries.forEach { reader.extractData(packageName, it, dataDir(packageName, userId, it)) }
            } else {
                Log.w(TAG, "Import: install of $packageName failed: ${result.msg}")
            }
            result.success
        } catch (e: Exception) {
            Log.e(TAG, "Import: restoring $packageName failed", e)
            false
        } finally {
            apk.delete()
        }
    }

    fun renameUser(userId: Int, name: String) {
        AppManager.mRemarkSharedPreferences.edit().apply {
            if (name.isBlank()) remove("Remark$userId") else putString("Remark$userId", name.trim())
            apply()
        }
    }

    suspend fun deleteUser(userId: Int) = withContext(Dispatchers.IO) {
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

    
    suspend fun updateApkOrder(userID: Int, dataList: List<AppInfo>) = withContext(Dispatchers.IO) {
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
