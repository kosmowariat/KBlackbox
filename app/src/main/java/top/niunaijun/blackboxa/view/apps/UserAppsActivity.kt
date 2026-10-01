package top.niunaijun.blackboxa.view.apps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.bean.UserBean
import top.niunaijun.blackboxa.databinding.ActivityUserAppsBinding
import top.niunaijun.blackboxa.util.InjectionUtil
import top.niunaijun.blackboxa.util.collectStarted
import top.niunaijun.blackboxa.util.Resolution
import top.niunaijun.blackboxa.util.inflate
import top.niunaijun.blackboxa.util.toast
import top.niunaijun.blackboxa.view.base.LoadingActivity
import top.niunaijun.blackboxa.view.fake.FakeManagerActivity
import top.niunaijun.blackboxa.view.list.ListActivity
import top.niunaijun.blackboxa.view.users.UserDialogs
import top.niunaijun.blackboxa.view.users.UsersEvent
import top.niunaijun.blackboxa.view.users.UsersViewModel

class UserAppsActivity : LoadingActivity() {

    private val viewBinding: ActivityUserAppsBinding by inflate()

    private val userId by lazy { intent.getIntExtra(EXTRA_USER_ID, 0) }

    private var userName = ""

    private lateinit var viewModel: UsersViewModel

    private lateinit var userDialogs: UserDialogs

    private val user: UserBean
        get() = UserBean(userId, userName, 0, emptyList())

    private val appsFragment: AppsFragment
        get() = supportFragmentManager.findFragmentById(R.id.fragmentContainer) as AppsFragment

    private val apkPathResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    it.data?.getStringExtra("source")?.let { source -> appsFragment.installApk(source) }
                }
            }

    private val exportResult =
            registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
                uri?.let {
                    showLoading()
                    viewModel.exportUser(userId, it, getString(R.string.backup_export_done))
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        initToolbar(viewBinding.toolbarLayout.toolbar, R.string.app_name, true)
        userName = savedInstanceState?.getString(EXTRA_USER_NAME) ?: intent.getStringExtra(EXTRA_USER_NAME).orEmpty()
        viewBinding.toolbarLayout.toolbar.title = userName
        initViewModel()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, AppsFragment.newInstance(userId))
                    .commit()
        }

        viewBinding.fab.setOnClickListener {
            val intent = Intent(this, ListActivity::class.java)
            intent.putExtra("userID", userId)
            apkPathResult.launch(intent)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_USER_NAME, userName)
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this, InjectionUtil.getUsersFactory())[UsersViewModel::class.java]
        userDialogs = UserDialogs(this, viewModel)
        collectStarted(viewModel.events) { event ->
            when (event) {
                is UsersEvent.DuplicateRequested -> userDialogs.showDuplicateDialog(event.request)
                is UsersEvent.UserReady -> {
                    hideLoading()
                    start(this, event.user.id, event.user.name)
                    finish()
                }
                is UsersEvent.Message -> {
                    hideLoading()
                    toast(event.text)
                }
                is UsersEvent.UserDeleted -> finish()
                is UsersEvent.LaunchResult -> Unit
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_user, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.user_rename -> userDialogs.showRenameDialog(user) { name ->
                userName = name
                viewBinding.toolbarLayout.toolbar.title = name
            }
            R.id.user_duplicate -> viewModel.requestDuplicate(user)
            R.id.user_delete -> userDialogs.showDeleteDialog(user)
            R.id.user_fake_location -> startActivity(
                    Intent(this, FakeManagerActivity::class.java).putExtra("userID", userId)
            )
            R.id.user_export -> exportResult.launch("$userName.zip")
            R.id.user_stop_all -> appsFragment.stopAllApps()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    fun showFloatButton(show: Boolean) {
        val translationY = if (show) 0f else Resolution.convertDpToPixel(120F, this)
        viewBinding.fab.animate()
                .translationY(translationY)
                .alpha(if (show) 1f else 0f)
                .setDuration(200L)
                .start()
    }

    companion object {
        private const val EXTRA_USER_ID = "userID"
        private const val EXTRA_USER_NAME = "userName"

        fun start(context: Context, userId: Int, userName: String) {
            val intent = Intent(context, UserAppsActivity::class.java)
            intent.putExtra(EXTRA_USER_ID, userId)
            intent.putExtra(EXTRA_USER_NAME, userName)
            context.startActivity(intent)
        }
    }
}
