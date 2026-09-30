package top.niunaijun.blackboxa.view.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        private val LEGACY_STORAGE_PERMISSIONS = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        fun start(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!runEngineHook { BlackBoxCore.get().onBeforeMainActivityOnCreate(this) }) {
            return
        }

        setContentView(viewBinding.root)
        initToolbar(viewBinding.toolbarLayout.toolbar, R.string.app_name)
        initUserList()
        initFab()
        checkStoragePermission()

        runEngineHook { BlackBoxCore.get().onAfterMainActivityOnCreate(this) }
    }

    /** Engine lifecycle hooks are the boundary where a failure must be shown to the user. */
    private fun runEngineHook(hook: () -> Unit): Boolean {
        return try {
            hook()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Engine hook failed in MainActivity", e)
            showErrorDialog(getString(R.string.init_failed, e.message.orEmpty()))
            false
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                return
            }
            if (PreferenceManager.getDefaultSharedPreferences(this)
                            .getBoolean(STORAGE_POSTPONED_KEY, false)) {
                Log.d(TAG, "Storage permission already postponed by the user")
                return
            }
            Log.w(TAG, "MANAGE_EXTERNAL_STORAGE permission not granted")
            showStoragePermissionDialog()
        } else if (!hasLegacyStoragePermissions()) {
            Log.w(TAG, "Storage permissions not granted on Android ${Build.VERSION.SDK_INT}")
            requestLegacyStoragePermission()
        }
    }

    private fun hasLegacyStoragePermissions(): Boolean {
        return LEGACY_STORAGE_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestLegacyStoragePermission() {
        ActivityCompat.requestPermissions(
                this,
                LEGACY_STORAGE_PERMISSIONS,
                STORAGE_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d(TAG, "Storage permissions granted")
            } else {
                Log.w(TAG, "Storage permissions denied")
            }
        }
    }

    private fun showStoragePermissionDialog() {
        MaterialAlertDialogBuilder(this)
                .setTitle(R.string.storage_permission_title)
                .setMessage(R.string.storage_permission_message)
                .setPositiveButton(R.string.storage_permission_grant) { _, _ -> openAllFilesAccessSettings() }
                .setNegativeButton(R.string.later) { _, _ ->
                    PreferenceManager.getDefaultSharedPreferences(this)
                            .edit { putBoolean(STORAGE_POSTPONED_KEY, true) }
                    Log.w(TAG, "User postponed storage permission")
                }
                .setCancelable(false)
                .show()
    }

    private fun openAllFilesAccessSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        val appIntent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                .setData(Uri.parse("package:$packageName"))
        try {
            storagePermissionResult.launch(appIntent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Per-app all-files settings unavailable, using the global screen", e)
            try {
                storagePermissionResult.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            } catch (e2: ActivityNotFoundException) {
                Log.e(TAG, "No screen to grant all-files access", e2)
                toast(R.string.storage_settings_unavailable)
            }
        }
    }

    private val storagePermissionResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        Log.d(TAG, "Storage permission granted!")
                    } else {
                        Log.w(TAG, "Storage permission still not granted")
                    }
                }
            }

    private fun showErrorDialog(message: String) {
        MaterialAlertDialogBuilder(this)
                .setTitle(R.string.error)
                .setMessage(message)
                .setPositiveButton(R.string.ok) { _, _ -> finish() }
                .setCancelable(false)
                .show()
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
        menuInflater.inflate(R.menu.menu_main, menu)
        menu?.findItem(R.id.main_layout)?.let { item ->
            val grid = AppManager.mBlackBoxLoader.userGridView()
            item.setIcon(if (grid) R.drawable.ic_view_list else R.drawable.ic_view_grid)
            item.setTitle(if (grid) R.string.list_view else R.string.grid_view)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.main_layout -> {
                val grid = !AppManager.mBlackBoxLoader.userGridView()
                AppManager.mBlackBoxLoader.invalidUserGridView(grid)
                applyUserLayout(grid)
                invalidateOptionsMenu()
            }
            R.id.main_git -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/kosmowariat/KBlackbox")))
            }
            R.id.main_setting -> SettingActivity.start(this)
            R.id.fake_location -> {
                startActivity(Intent(this, FakeManagerActivity::class.java).putExtra("userID", 0))
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
