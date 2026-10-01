package top.niunaijun.blackboxa.view.apps

import android.graphics.Point
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.bean.AppDetails
import top.niunaijun.blackboxa.bean.AppInfo
import top.niunaijun.blackboxa.bean.UserBean
import top.niunaijun.blackboxa.databinding.FragmentAppsBinding
import top.niunaijun.blackboxa.databinding.ItemAppBinding
import top.niunaijun.blackboxa.util.BindingAdapter
import top.niunaijun.blackboxa.util.InjectionUtil
import top.niunaijun.blackboxa.util.MemoryManager
import top.niunaijun.blackboxa.util.ShortcutUtil
import top.niunaijun.blackboxa.util.collectStarted
import top.niunaijun.blackboxa.util.inflate
import top.niunaijun.blackboxa.util.showConfirmDialog
import top.niunaijun.blackboxa.util.toast
import top.niunaijun.blackboxa.view.base.LoadingActivity
import kotlin.math.abs

class AppsFragment : Fragment() {

    var userID: Int = 0

    private lateinit var viewModel: AppsViewModel

    private lateinit var mAdapter: BindingAdapter<AppInfo, ItemAppBinding>

    private val viewBinding: FragmentAppsBinding by inflate()

    private var running: Set<String> = emptySet()

    companion object {
        private const val TAG = "AppsFragment"
        private const val GRID_SPAN_COUNT = 4
        private const val VERTICAL_SCROLL_THRESHOLD = 10
        private const val FAST_SCROLL_DY = 100

        fun newInstance(userID: Int): AppsFragment {
            val fragment = AppsFragment()
            fragment.arguments = bundleOf("userID" to userID)
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, InjectionUtil.getAppsFactory())[AppsViewModel::class.java]
        userID = requireArguments().getInt("userID", 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding.stateView.showEmpty()

        mAdapter = appsAdapter { it.packageName in running }
        viewBinding.recyclerView.adapter = mAdapter
        viewBinding.recyclerView.layoutManager = GridLayoutManager(requireContext(), GRID_SPAN_COUNT).apply {
            isItemPrefetchEnabled = true
            initialPrefetchItemCount = 8
        }
        viewBinding.recyclerView.setItemViewCacheSize(20)
        viewBinding.recyclerView.setHasFixedSize(true)
        viewBinding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    MemoryManager.optimizeMemoryForRecyclerView()
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (abs(dy) > FAST_SCROLL_DY && MemoryManager.isMemoryCritical()) {
                    Log.w(TAG, "Memory critical during fast scrolling, forcing GC")
                    MemoryManager.forceGarbageCollectionIfNeeded()
                }
            }
        })

        val touchCallBack = AppsTouchCallBack(
            onMoveBlock = { from, to ->
                onItemMove(from, to)
                viewModel.updateApkOrder(userID, mAdapter.getItems())
            },
            onLongPressRelease = { view, position -> showAppMenu(view, mAdapter.getItems()[position]) }
        )
        ItemTouchHelper(touchCallBack).attachToRecyclerView(viewBinding.recyclerView)

        mAdapter.onItemClick = { _, data, _ ->
            ShortcutUtil.pushRecentApp(requireContext(), userID, data)
            showLoading()
            viewModel.launchApk(data.packageName, userID)
        }

        interceptTouch()
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
    }

    override fun onStart() {
        super.onStart()
        BlackBoxCore.get().addServiceAvailableCallback {
            Log.d(TAG, "Services became available, refreshing app list")
            viewModel.getInstalledAppsWithRetry(userID)
        }
        viewModel.getInstalledAppsWithRetry(userID)
    }

    private fun interceptTouch() {
        val point = Point()

        viewBinding.recyclerView.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP -> point.set(0, 0)

                MotionEvent.ACTION_MOVE -> {
                    if (point.x == 0 && point.y == 0) {
                        point.set(e.rawX.toInt(), e.rawY.toInt())
                    }
                    updateFloatButton(point, e)
                }
            }
            false
        }
    }

    private fun updateFloatButton(point: Point, e: MotionEvent) {
        val dy = point.y - e.rawY
        if (abs(dy) > VERTICAL_SCROLL_THRESHOLD) {
            (requireActivity() as? UserAppsActivity)?.showFloatButton(dy < 0)
        }
    }

    private fun onItemMove(fromPosition: Int, toPosition: Int) {
        val size = mAdapter.itemCount
        if (fromPosition !in 0 until size || toPosition !in 0 until size) {
            Log.w(TAG, "Invalid positions for move: from=$fromPosition, to=$toPosition, size=$size")
            return
        }
        mAdapter.moveItem(fromPosition, toPosition)
    }

    private fun showAppMenu(view: View, data: AppInfo) {
        PopupMenu(requireContext(), view).also {
            it.inflate(R.menu.app_menu)
            it.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.app_remove -> unInstallApk(data)

                    R.id.app_clear -> clearApk(data)

                    R.id.app_stop -> stopApk(data)

                    R.id.app_shortcut -> ShortcutUtil.createShortcut(requireContext(), userID, data)

                    R.id.app_copy -> viewModel.requestCopy(data, userID)

                    R.id.app_info -> viewModel.showDetails(data, userID)
                }
                true
            }
            it.show()
        }
    }

    private fun initData() {
        viewBinding.stateView.showLoading()
        viewModel.getInstalledApps(userID)

        viewLifecycleOwner.collectStarted(viewModel.apps) { apps ->
            if (apps != null) {
                mAdapter.setItems(apps)
                viewModel.refreshRunning(userID)
                if (apps.isEmpty()) {
                    viewBinding.stateView.showEmpty()
                } else {
                    viewBinding.stateView.showContent()
                }
            }
        }

        viewLifecycleOwner.collectStarted(viewModel.running) { packages ->
            running = packages
            mAdapter.notifyItemRangeChanged(0, mAdapter.itemCount, Unit)
        }

        viewLifecycleOwner.collectStarted(viewModel.events) { event ->
            hideLoading()
            when (event) {
                is AppsEvent.Notice -> requireContext().toast(event.text)
                is AppsEvent.Details -> showDetails(event.details)
                is AppsEvent.CopyTargets -> showCopyDialog(event.app, event.targets)
                is AppsEvent.Message -> {
                    requireContext().toast(event.text)
                    viewModel.getInstalledApps(userID)
                }
                is AppsEvent.LaunchResult -> {
                    if (!event.launched) toast(R.string.start_fail)
                    viewModel.refreshRunning(userID)
                }
            }
        }
    }

    private fun unInstallApk(info: AppInfo) {
        requireContext().showConfirmDialog(
                R.string.uninstall_app,
                getString(R.string.uninstall_app_hint, info.name)
        ) {
            ShortcutUtil.removeShortcuts(requireContext(), userID, info)
            showLoading()
            viewModel.unInstall(info.packageName, userID)
        }
    }

    private fun stopApk(info: AppInfo) {
        requireContext().showConfirmDialog(
                R.string.app_stop,
                getString(R.string.app_stop_hint, info.name)
        ) {
            try {
                BlackBoxCore.get().stopPackage(info.packageName, userID)
                toast(getString(R.string.is_stop, info.name))
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping ${info.packageName}", e)
                toast(getString(R.string.stop_error, info.name))
            }
        }
    }

    private fun clearApk(info: AppInfo) {
        requireContext().showConfirmDialog(
                R.string.app_clear,
                getString(R.string.app_clear_hint, info.name)
        ) {
            showLoading()
            viewModel.clearApkData(info.packageName, userID)
        }
    }

    private fun showDetails(details: AppDetails) {
        val message = listOf(
                getString(R.string.app_info_package, details.packageName),
                getString(R.string.app_info_version, details.versionName, details.versionCode),
                getString(R.string.app_info_data, Formatter.formatShortFileSize(requireContext(), details.dataBytes))
        ).joinToString("\n")
        MaterialAlertDialogBuilder(requireContext())
                .setTitle(details.name)
                .setMessage(message)
                .setPositiveButton(R.string.done, null)
                .show()
    }

    private fun showCopyDialog(app: AppInfo, targets: List<UserBean>) {
        if (targets.isEmpty()) {
            toast(R.string.app_copy_no_spaces)
            return
        }
        MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.app_copy_title, app.name))
                .setItems(targets.map { it.name }.toTypedArray()) { _, index -> askCopyData(app, targets[index]) }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    private fun askCopyData(app: AppInfo, target: UserBean) {
        MaterialAlertDialogBuilder(requireContext())
                .setTitle(app.name)
                .setMessage(R.string.app_copy_data_question)
                .setPositiveButton(R.string.app_copy_with_data) { _, _ -> copyApp(app, target, true) }
                .setNegativeButton(R.string.app_copy_without_data) { _, _ -> copyApp(app, target, false) }
                .setNeutralButton(R.string.cancel, null)
                .show()
    }

    private fun copyApp(app: AppInfo, target: UserBean, withData: Boolean) {
        showLoading()
        viewModel.copyApp(app, userID, target, withData)
    }

    fun stopAllApps() {
        viewModel.stopAll(userID, getString(R.string.all_apps_stopped))
    }

    fun installApk(source: String) {
        showLoading()
        viewModel.install(source, userID)
    }

    private fun showLoading() {
        (activity as? LoadingActivity)?.showLoading()
    }

    private fun hideLoading() {
        (activity as? LoadingActivity)?.hideLoading()
    }
}
