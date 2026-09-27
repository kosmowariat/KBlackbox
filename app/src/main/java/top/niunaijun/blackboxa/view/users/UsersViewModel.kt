package top.niunaijun.blackboxa.view.users

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import top.niunaijun.blackboxa.bean.UserBean
import top.niunaijun.blackboxa.data.AppsRepository
import top.niunaijun.blackboxa.view.base.BaseViewModel

class UsersViewModel(private val repo: AppsRepository) : BaseViewModel() {

    private val mUsers = MutableLiveData<List<UserBean>>()
    val users: LiveData<List<UserBean>> = mUsers

    private val mCreatedUser = MutableLiveData<UserBean?>()
    val createdUser: LiveData<UserBean?> = mCreatedUser

    fun loadUsers() {
        launchOnUI { repo.getUserList(mUsers) }
    }

    private val mLaunchResult = MutableLiveData<Boolean?>()
    val launchResult: LiveData<Boolean?> = mLaunchResult

    fun createUser(name: String) {
        launchOnUI { repo.createUser(name, mCreatedUser) }
    }

    fun launchApp(packageName: String, userId: Int) {
        launchOnUI { mLaunchResult.postValue(repo.launchApk(packageName, userId)) }
    }

    fun onLaunchResultHandled() {
        mLaunchResult.value = null
    }

    fun onCreatedUserOpened() {
        mCreatedUser.value = null
    }

    fun renameUser(userId: Int, name: String) {
        launchOnUI {
            repo.renameUser(userId, name)
            repo.getUserList(mUsers)
        }
    }

    fun deleteUser(userId: Int) {
        launchOnUI {
            repo.deleteUser(userId)
            repo.getUserList(mUsers)
        }
    }
}
