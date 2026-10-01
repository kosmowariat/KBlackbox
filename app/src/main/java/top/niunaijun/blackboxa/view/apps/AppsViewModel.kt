package top.niunaijun.blackboxa.view.apps

import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import top.niunaijun.blackboxa.bean.AppDetails
import top.niunaijun.blackboxa.bean.AppInfo
import top.niunaijun.blackboxa.bean.UserBean
import top.niunaijun.blackboxa.data.AppsRepository
import top.niunaijun.blackboxa.view.base.BaseViewModel

sealed interface AppsEvent {
    data class Message(val text: String) : AppsEvent
    data class Notice(val text: String) : AppsEvent
    data class LaunchResult(val launched: Boolean) : AppsEvent
    data class Details(val details: AppDetails) : AppsEvent
    data class CopyTargets(val app: AppInfo, val targets: List<UserBean>) : AppsEvent
}

class AppsViewModel(private val repo: AppsRepository) : BaseViewModel() {

    /** The apps of the current user; null until the first load finishes. */
    private val _apps = MutableStateFlow<List<AppInfo>?>(null)
    val apps: StateFlow<List<AppInfo>?> = _apps.asStateFlow()

    /** Packages of the current user that have a live task in the sandbox. */
    private val _running = MutableStateFlow<Set<String>>(emptySet())
    val running: StateFlow<Set<String>> = _running.asStateFlow()

    private val _events = Channel<AppsEvent>(Channel.BUFFERED)
    val events: Flow<AppsEvent> = _events.receiveAsFlow()

    fun getInstalledApps(userId: Int) {
        launch { _apps.value = repo.getVmInstallList(userId) }
    }

    /** The engine services may not be ready right after start, so an empty list is retried a few times. */
    fun getInstalledAppsWithRetry(userId: Int, maxRetries: Int = 3) {
        launch {
            var attempt = 0
            while (true) {
                val list = repo.getVmInstallList(userId)
                _apps.value = list
                if (list.isNotEmpty() || attempt >= maxRetries) {
                    break
                }
                attempt++
                Log.d(TAG, "No apps loaded, retrying... ($attempt/$maxRetries)")
                delay(RETRY_DELAY_MS)
            }
        }
    }

    fun refreshRunning(userId: Int) {
        val packages = _apps.value.orEmpty().map { it.packageName }
        launch { _running.value = repo.getRunningPackages(userId, packages) }
    }

    fun stopAll(userId: Int, messageAfter: String) {
        val packages = _apps.value.orEmpty().map { it.packageName }
        launch {
            repo.stopAll(userId, packages)
            _running.value = emptySet()
            _events.send(AppsEvent.Notice(messageAfter))
        }
    }

    fun install(source: String, userID: Int) {
        launch { _events.send(AppsEvent.Message(repo.installApk(source, userID))) }
    }

    fun unInstall(packageName: String, userID: Int) {
        launch { _events.send(AppsEvent.Message(repo.unInstall(packageName, userID))) }
    }

    fun clearApkData(packageName: String, userID: Int) {
        launch { _events.send(AppsEvent.Message(repo.clearApkData(packageName, userID))) }
    }

    fun launchApk(packageName: String, userID: Int) {
        launch { _events.send(AppsEvent.LaunchResult(repo.launchApk(packageName, userID))) }
    }

    fun showDetails(info: AppInfo, userId: Int) {
        launch { _events.send(AppsEvent.Details(repo.getAppDetails(info, userId))) }
    }

    fun requestCopy(info: AppInfo, userId: Int) {
        launch { _events.send(AppsEvent.CopyTargets(info, repo.getCopyTargets(userId))) }
    }

    fun copyApp(info: AppInfo, userId: Int, target: UserBean, withData: Boolean) {
        launch { _events.send(AppsEvent.Notice(repo.copyApp(info, userId, target, withData))) }
    }

    fun updateApkOrder(userID: Int, dataList: List<AppInfo>) {
        val snapshot = dataList.toList()
        launch { repo.updateApkOrder(userID, snapshot) }
    }

    private companion object {
        const val TAG = "AppsViewModel"
        const val RETRY_DELAY_MS = 1000L
    }
}
