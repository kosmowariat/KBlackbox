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
import top.niunaijun.blackboxa.util.Resolution
import top.niunaijun.blackboxa.util.inflate
import top.niunaijun.blackboxa.util.toast
import top.niunaijun.blackboxa.view.base.LoadingActivity
import top.niunaijun.blackboxa.view.list.ListActivity
import top.niunaijun.blackboxa.view.users.UserDialogs
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
        viewModel.duplicateRequest.observe(this) { request ->
            request ?: return@observe
            viewModel.onDuplicateRequestShown()
            userDialogs.showDuplicateDialog(request)
        }
        viewModel.createdUser.observe(this) { created ->
            created ?: return@observe
            viewModel.onCreatedUserOpened()
            hideLoading()
            start(this, created.id, created.name)
            finish()
        }
        viewModel.createError.observe(this) { message ->
            message ?: return@observe
            viewModel.onCreateErrorShown()
            hideLoading()
            toast(message)
        }
        viewModel.deletedUser.observe(this) { deleted ->
            deleted ?: return@observe
            viewModel.onDeletedUserHandled()
            finish()
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
