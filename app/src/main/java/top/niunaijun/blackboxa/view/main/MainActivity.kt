package top.niunaijun.blackboxa.view.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.app.AppManager
import top.niunaijun.blackboxa.databinding.ActivityMainBinding
import top.niunaijun.blackboxa.util.InjectionUtil
import top.niunaijun.blackboxa.util.inflate
import top.niunaijun.blackboxa.util.toast
import top.niunaijun.blackboxa.view.apps.UserAppsActivity
import top.niunaijun.blackboxa.view.base.LoadingActivity
import top.niunaijun.blackboxa.view.fake.FakeManagerActivity
import top.niunaijun.blackboxa.view.setting.SettingActivity
import top.niunaijun.blackboxa.view.users.UserDialogs
import top.niunaijun.blackboxa.view.users.UsersAdapter
import top.niunaijun.blackboxa.view.users.UsersViewModel

class MainActivity : LoadingActivity() {

    private val viewBinding: ActivityMainBinding by inflate()

    private lateinit var viewModel: UsersViewModel

    private lateinit var mAdapter: UsersAdapter

    private lateinit var userDialogs: UserDialogs

    companion object {
        private const val TAG = "MainActivity"
        private const val STORAGE_PERMISSION_REQUEST_CODE = 1001
        private const val STORAGE_POSTPONED_KEY = "storage_permission_postponed"

        fun start(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)

            try {
                BlackBoxCore.get().onBeforeMainActivityOnCreate(this)
            } catch (e: Exception) {
                Log.e(TAG, "Error in onBeforeMainActivityOnCreate: ${e.message}")
            }

            setContentView(viewBinding.root)
            initToolbar(viewBinding.toolbarLayout.toolbar, R.string.app_name)
            initUserList()
            initFab()

            
            checkStoragePermission()

            try {
                BlackBoxCore.get().onAfterMainActivityOnCreate(this)
            } catch (e: Exception) {
                Log.e(TAG, "Error in onAfterMainActivityOnCreate: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in onCreate: ${e.message}")
            
            showErrorDialog(getString(R.string.init_failed, e.message))
        }
    }

    private fun checkStoragePermission() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                
                if (!android.os.Environment.isExternalStorageManager()) {
                    if (PreferenceManager.getDefaultSharedPreferences(this)
                                    .getBoolean(STORAGE_POSTPONED_KEY, false)) {
                        Log.d(TAG, "Storage permission already postponed by the user")
                        return
                    }
                    Log.w(TAG, "MANAGE_EXTERNAL_STORAGE permission not granted")
                    showStoragePermissionDialog()
                }
            } else {
                
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                                this,
                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED ||
                                androidx.core.content.ContextCompat.checkSelfPermission(
                                        this,
                                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(
                            TAG,
                            "Storage permissions not granted on Android ${android.os.Build.VERSION.SDK_INT}"
                    )
                    requestLegacyStoragePermission()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking storage permission: ${e.message}")
        }
    }

    private fun requestLegacyStoragePermission() {
        try {
            androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    STORAGE_PERMISSION_REQUEST_CODE
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting storage permission: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                            grantResults.all {
                                it == android.content.pm.PackageManager.PERMISSION_GRANTED
                            }
            ) {
                Log.d(TAG, "Storage permissions granted")
            } else {
                Log.w(TAG, "Storage permissions denied")
            }
        }
    }

    private fun showStoragePermissionDialog() {
        try {
            MaterialDialog(this).show {
                title(R.string.storage_permission_title)
                message(R.string.storage_permission_message)
                positiveButton(R.string.storage_permission_grant) { openAllFilesAccessSettings() }
                negativeButton(R.string.later) {
                    PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
                            .edit { putBoolean(STORAGE_POSTPONED_KEY, true) }
                    Log.w(TAG, "User postponed storage permission")
                }
                cancelable(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing storage permission dialog: ${e.message}")
        }
    }

    private fun openAllFilesAccessSettings() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val intent =
                        Intent(
                                android.provider.Settings
                                        .ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                        )
                intent.data = Uri.parse("package:$packageName")
                storagePermissionResult.launch(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening storage settings: ${e.message}")
            
            try {
                val intent =
                        Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                storagePermissionResult.launch(intent)
            } catch (e2: Exception) {
                Log.e(TAG, "Error opening fallback storage settings: ${e2.message}")
            }
        }
    }

    private val storagePermissionResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        if (android.os.Environment.isExternalStorageManager()) {
                            Log.d(TAG, "Storage permission granted!")
                        } else {
                            Log.w(TAG, "Storage permission still not granted")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling storage permission result: ${e.message}")
                }
            }

    private fun showErrorDialog(message: String) {
        try {
            MaterialDialog(this).show {
                title(R.string.error)
                message(text = message)
                positiveButton(R.string.ok) { finish() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing error dialog: ${e.message}")
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.loadUsers()
        }
    }

    private fun initUserList() {
        viewModel = ViewModelProvider(this, InjectionUtil.getUsersFactory())[UsersViewModel::class.java]
        userDialogs = UserDialogs(this, viewModel)
        mAdapter = UsersAdapter(
                onClick = { UserAppsActivity.start(this, it.id, it.name) },
                onAppClick = { user, app ->
                    showLoading()
                    viewModel.launchApp(app.packageName, user.id)
                },
                onRename = { userDialogs.showRenameDialog(it) },
                onDuplicate = { viewModel.requestDuplicate(it) },
                onDelete = { userDialogs.showDeleteDialog(it) }
        )
        viewBinding.recyclerView.adapter = mAdapter
        applyUserLayout(AppManager.mBlackBoxLoader.userGridView())
        viewModel.users.observe(this) { mAdapter.submitList(it) }
        viewModel.createdUser.observe(this) { user ->
            user ?: return@observe
            viewModel.onCreatedUserOpened()
            hideLoading()
            UserAppsActivity.start(this, user.id, user.name)
        }
        viewModel.duplicateRequest.observe(this) { request ->
            request ?: return@observe
            viewModel.onDuplicateRequestShown()
            userDialogs.showDuplicateDialog(request)
        }
        viewModel.createError.observe(this) { message ->
            message ?: return@observe
            viewModel.onCreateErrorShown()
            hideLoading()
            toast(message)
        }
        viewModel.launchResult.observe(this) { launched ->
            launched ?: return@observe
            viewModel.onLaunchResultHandled()
            hideLoading()
            if (!launched) {
                toast(R.string.start_fail)
            }
        }
    }

    private fun applyUserLayout(grid: Boolean) {
        mAdapter.gridMode = grid
        viewBinding.recyclerView.layoutManager = if (grid) {
            GridLayoutManager(this, resources.getInteger(R.integer.user_grid_columns))
        } else {
            LinearLayoutManager(this)
        }
    }

    private fun initFab() {
        viewBinding.fab.setOnClickListener {
            userDialogs.showCreateDialog()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        try {
            menuInflater.inflate(R.menu.menu_main, menu)
            menu?.findItem(R.id.main_layout)?.let { item ->
                val grid = AppManager.mBlackBoxLoader.userGridView()
                item.setIcon(if (grid) R.drawable.ic_view_list else R.drawable.ic_view_grid)
                item.setTitle(if (grid) R.string.list_view else R.string.grid_view)
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating options menu: ${e.message}")
            return false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        try {
            when (item.itemId) {
                R.id.main_layout -> {
                    val grid = !AppManager.mBlackBoxLoader.userGridView()
                    AppManager.mBlackBoxLoader.invalidUserGridView(grid)
                    applyUserLayout(grid)
                    invalidateOptionsMenu()
                }
                R.id.main_git -> {
                    val intent =
                            Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/ALEX5402/NewBlackbox")
                            )
                    startActivity(intent)
                }
                R.id.main_setting -> {
                    SettingActivity.start(this)
                }
                R.id.fake_location -> {
                    
                    val intent = Intent(this, FakeManagerActivity::class.java)
                    intent.putExtra("userID", 0)
                    startActivity(intent)
                }
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error handling menu item selection: ${e.message}")
            return false
        }
    }
}
