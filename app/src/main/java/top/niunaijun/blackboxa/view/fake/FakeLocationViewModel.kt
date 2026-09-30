package top.niunaijun.blackboxa.view.fake

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import top.niunaijun.blackbox.entity.location.BLocation
import top.niunaijun.blackboxa.bean.FakeLocationBean
import top.niunaijun.blackboxa.data.FakeLocationRepository
import top.niunaijun.blackboxa.view.base.BaseViewModel

class FakeLocationViewModel(private val mRepo: FakeLocationRepository) : BaseViewModel() {

    /** The apps with their fake location; null until the first load finishes. */
    private val _apps = MutableStateFlow<List<FakeLocationBean>?>(null)
    val apps: StateFlow<List<FakeLocationBean>?> = _apps.asStateFlow()

    fun getInstallAppList(userID: Int) {
        launch { _apps.value = mRepo.getInstalledAppList(userID) }
    }

    fun setPattern(userId: Int, pkg: String, pattern: Int) {
        launch { mRepo.setPattern(userId, pkg, pattern) }
    }

    fun setLocation(userId: Int, pkg: String, location: BLocation) {
        launch { mRepo.setLocation(userId, pkg, location) }
    }
}
