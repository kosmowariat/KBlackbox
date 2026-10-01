package top.niunaijun.blackboxa.view.users

import android.net.Uri
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import top.niunaijun.blackboxa.bean.DuplicateUserBean
import top.niunaijun.blackboxa.bean.UserBean
import top.niunaijun.blackboxa.bean.UserCreationResult
import top.niunaijun.blackboxa.data.AppsRepository
import top.niunaijun.blackboxa.view.base.BaseViewModel

/** One-shot results the screen reacts to once, such as opening a screen or showing a message. */
sealed interface UsersEvent {
    data class UserReady(val user: UserBean) : UsersEvent
    data class Message(val text: String) : UsersEvent
    data class DuplicateRequested(val request: DuplicateUserBean) : UsersEvent
    data class LaunchResult(val launched: Boolean) : UsersEvent
    data class UserDeleted(val userId: Int) : UsersEvent
}

class UsersViewModel(private val repo: AppsRepository) : BaseViewModel() {

    private val _users = MutableStateFlow<List<UserBean>>(emptyList())
    val users: StateFlow<List<UserBean>> = _users.asStateFlow()

    private val _events = Channel<UsersEvent>(Channel.BUFFERED)
    val events: Flow<UsersEvent> = _events.receiveAsFlow()

    val isGmsSupported: Boolean
        get() = repo.isGmsSupported()

    fun loadUsers() {
        launch { _users.value = repo.getUserList() }
    }

    fun createUser(name: String, installGms: Boolean) {
        launch { publish(repo.createUser(name, installGms)) }
    }

    fun requestDuplicate(user: UserBean) {
        launch { _events.send(UsersEvent.DuplicateRequested(repo.getDuplicateRequest(user))) }
    }

    fun duplicateUser(sourceUserId: Int, name: String, copyDataFor: Set<String>) {
        launch { publish(repo.duplicateUser(sourceUserId, name, copyDataFor)) }
    }

    fun exportUser(userId: Int, target: Uri, successMessage: String) {
        launch { _events.send(UsersEvent.Message(repo.exportUser(userId, target) ?: successMessage)) }
    }

    fun importUser(source: Uri) {
        launch { publish(repo.importUser(source)) }
    }

    fun launchApp(packageName: String, userId: Int) {
        launch { _events.send(UsersEvent.LaunchResult(repo.launchApk(packageName, userId))) }
    }

    fun renameUser(userId: Int, name: String) {
        launch {
            repo.renameUser(userId, name)
            _users.value = repo.getUserList()
        }
    }

    fun deleteUser(userId: Int) {
        launch {
            repo.deleteUser(userId)
            _events.send(UsersEvent.UserDeleted(userId))
            _users.value = repo.getUserList()
        }
    }

    private suspend fun publish(result: UserCreationResult) {
        result.error?.let { _events.send(UsersEvent.Message(it)) }
        result.user?.let { _events.send(UsersEvent.UserReady(it)) }
    }
}
