package top.niunaijun.blackboxa.view.apps

import android.graphics.Point
import android.os.Bundle
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
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.bean.AppInfo
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

    private var popupMenu: PopupMenu? = null

    private var running: Set<String> = emptySet()

    companion object {
        private const val TAG = "AppsFragment"
        private const val GRID_SPAN_COUNT = 4
        private const val MOVE_THRESHOLD = 40
        private const val VERTICAL_SCROLL_THRESHOLD = 10
        private const val TAP_MAX_DURATION_MS = 500
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

        val touchCallBack = AppsTouchCallBack { from, to ->
            onItemMove(from, to)
            viewModel.updateApkOrder(userID, mAdapter.getItems())
        }
        ItemTouchHelper(touchCallBack).attachToRecyclerView(viewBinding.recyclerView)

        mAdapter.onItemClick = { _, data, _ ->
            ShortcutUtil.pushRecentApp(requireContext(), userID, data)
            showLoading()
            viewModel.launchApk(data.packageName, userID)
        }

        interceptTouch()
        setOnLongClick()
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

    /**
     * The context menu opens on release, so it doesn't fight with drag-to-reorder: a press that
     * turns into a scroll or drag dismisses it instead.
     */
    private fun interceptTouch() {
        val point = Point()
        var isScrolling = false
        var scrollStartTime = 0L

        viewBinding.recyclerView.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    isScrolling = false
                    scrollStartTime = System.currentTimeMillis()
                    point.set(0, 0)
                }

                MotionEvent.ACTION_UP -> {
                    val pressDuration = System.currentTimeMillis() - scrollStartTime
                    if (!isScrolling && !isMove(point, e) && pressDuration < TAP_MAX_DURATION_MS) {
                        popupMenu?.show()
                    }
                    popupMenu = null
                    point.set(0, 0)
                    isScrolling = false
                }

                MotionEvent.ACTION_MOVE -> {
                    if (point.x == 0 && point.y == 0) {
                        point.set(e.rawX.toInt(), e.rawY.toInt())
                    }
                    if (isMove(point, e)) {
                        isScrolling = true
                        popupMenu?.dismiss()
                    }
                    updateFloatButton(point, e)
                }
            }
            false
        }
    }

    private fun isMove(point: Point, e: MotionEvent): Boolean {
        return abs(point.x - e.rawX) > MOVE_THRESHOLD || abs(point.y - e.rawY) > MOVE_THRESHOLD
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

    private fun setOnLongClick() {
        mAdapter.onItemLongClick = { view, data, _ ->
            popupMenu = PopupMenu(requireContext(), view).also {
                it.inflate(R.menu.app_menu)
                it.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.app_remove -> unInstallApk(data)

                        R.id.app_clear -> clearApk(data)

                        R.id.app_stop -> stopApk(data)

                        R.id.app_shortcut -> ShortcutUtil.createShortcut(requireContext(), userID, data)
                    }
                    true
                }
                it.show()
            }
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
