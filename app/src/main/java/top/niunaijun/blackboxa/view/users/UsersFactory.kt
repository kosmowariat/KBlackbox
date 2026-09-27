package top.niunaijun.blackboxa.view.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import top.niunaijun.blackboxa.data.AppsRepository

@Suppress("UNCHECKED_CAST")
class UsersFactory(private val appsRepository: AppsRepository) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return UsersViewModel(appsRepository) as T
    }
}
