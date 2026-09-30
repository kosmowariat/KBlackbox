package top.niunaijun.blackboxa.view.list

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import top.niunaijun.blackboxa.bean.InstalledAppBean
import top.niunaijun.blackboxa.data.AppsRepository
import top.niunaijun.blackboxa.view.base.BaseViewModel

class ListViewModel(private val repo: AppsRepository) : BaseViewModel() {

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** The apps installed on the phone; null until the first load finishes. */
    private val _apps = MutableStateFlow<List<InstalledAppBean>?>(null)
    val apps: StateFlow<List<InstalledAppBean>?> = _apps.asStateFlow()

    fun previewInstalledList() {
        launch { repo.previewInstallList() }
    }

    fun getInstallAppList(userID: Int) {
        launch {
            _loading.value = true
            val apps = repo.getInstalledAppList(userID)
            _apps.value = apps
            _loading.value = false
            if (apps.isNotEmpty()) {
                repo.previewInstallList()
            }
        }
    }
}
