package top.niunaijun.blackboxa.view.gms

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import top.niunaijun.blackboxa.bean.GmsBean
import top.niunaijun.blackboxa.bean.GmsInstallBean
import top.niunaijun.blackboxa.data.GmsRepository
import top.niunaijun.blackboxa.view.base.BaseViewModel

class GmsViewModel(private val mRepo: GmsRepository) : BaseViewModel() {

    /** The users with their GMS state; null until the first load finishes. */
    private val _users = MutableStateFlow<List<GmsBean>?>(null)
    val users: StateFlow<List<GmsBean>?> = _users.asStateFlow()

    private val _installResults = Channel<GmsInstallBean>(Channel.BUFFERED)
    val installResults: Flow<GmsInstallBean> = _installResults.receiveAsFlow()

    fun getInstalledUser() {
        launch { _users.value = mRepo.getGmsInstalledList() }
    }

    fun installGms(userID: Int) {
        launch { _installResults.send(mRepo.installGms(userID)) }
    }

    fun uninstallGms(userID: Int) {
        launch { _installResults.send(mRepo.uninstallGms(userID)) }
    }
}
