package top.niunaijun.blackboxa.view.fake

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import top.niunaijun.blackbox.entity.location.BLocation
import top.niunaijun.blackbox.fake.frameworks.BLocationManager
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.bean.FakeLocationBean
import top.niunaijun.blackboxa.databinding.ActivityListBinding
import top.niunaijun.blackboxa.databinding.ItemFakeBinding
import top.niunaijun.blackboxa.util.BindingAdapter
import top.niunaijun.blackboxa.util.InjectionUtil
import top.niunaijun.blackboxa.util.collectStarted
import top.niunaijun.blackboxa.util.inflate
import top.niunaijun.blackboxa.util.showConfirmDialog
import top.niunaijun.blackboxa.util.toast
import top.niunaijun.blackboxa.view.base.BaseActivity


class FakeManagerActivity : BaseActivity() {
    val TAG: String = "FakeManagerActivity"

    private val viewBinding: ActivityListBinding by inflate()

    private lateinit var mAdapter: BindingAdapter<FakeLocationBean, ItemFakeBinding>

    private lateinit var viewModel: FakeLocationViewModel

    private var appList: List<FakeLocationBean> = ArrayList()

    private var query = ""

    private var userId = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        userId = currentUserID()
        initToolbar(viewBinding.toolbarLayout.toolbar, R.string.fake_location, true)

        mAdapter = fakeLocationAdapter()
        mAdapter.onItemClick = { _, data, _ ->
            val intent = Intent(this, FollowMyLocationOverlay::class.java)
            intent.putExtra("location", data.fakeLocation)
            intent.putExtra("pkg", data.packageName)

            locationResult.launch(intent)
        }
        mAdapter.onItemLongClick = { _, item, position -> disableFakeLocation(item, position) }
        viewBinding.recyclerView.adapter = mAdapter
        viewBinding.recyclerView.layoutManager = LinearLayoutManager(this)

        initViewModel()
    }

    private fun disableFakeLocation(item: FakeLocationBean,position:Int) {
        showConfirmDialog(
                R.string.close_fake_location,
                getString(R.string.close_app_fake_location, item.name)
        ) {
            BLocationManager.disableFakeLocation(userId, item.packageName)
            toast(getString(R.string.close_fake_location_success, item.name))
            item.fakeLocationPattern = BLocationManager.CLOSE_MODE
            mAdapter.replaceAt(position, item)
        }
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this, InjectionUtil.getFakeLocationFactory()).get(
            FakeLocationViewModel::class.java
        )
        loadAppList()
        viewModel.loadSpaces()
        viewBinding.toolbarLayout.toolbar.setTitle(R.string.fake_location)

        collectStarted(viewModel.apps) { apps ->
            if (apps != null) {
                appList = apps
                filterApp(query)
                if (apps.isNotEmpty()) {
                    viewBinding.stateView.showContent()
                } else {
                    viewBinding.stateView.showEmpty()
                }
            }
        }
    }

    private fun loadAppList() {
        viewBinding.stateView.showLoading()
        viewBinding.toolbarLayout.toolbar.subtitle = viewModel.spaceName(userId)
        viewModel.getInstallAppList(userId)
    }

    private val locationResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {

                it.data?.let { data ->
                    val latitude = data.getDoubleExtra("latitude", 0.0)
                    val longitude = data.getDoubleExtra("longitude", 0.0)
                    val pkg = data.getStringExtra("pkg")

                    viewModel.setPattern(userId, pkg.toString(), BLocationManager.OWN_MODE)
                    viewModel.setLocation(userId, pkg.toString(), BLocation(latitude, longitude))

                    toast(getString(R.string.set_location,latitude.toString(), longitude.toString()))

                    loadAppList()
                }

            }
        }


    private fun filterApp(newText: String) {
        val newList = this.appList.filter {
            it.name.contains(newText, true) or it.packageName.contains(newText, true)
        }
        mAdapter.setItems(newList)
    }

    private fun finishWithResult(source: String) {
        intent.putExtra("source", source)
        setResult(Activity.RESULT_OK, intent)
        val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        window.peekDecorView()?.run {
            imm.hideSoftInputFromWindow(windowToken, 0)
        }
        finish()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_fake, menu)
        val searchView = menu!!.findItem(R.id.list_search).actionView as SearchView
        searchView.queryHint = getString(R.string.filter)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                query = newText.orEmpty()
                filterApp(query)
                return true
            }

            override fun onQueryTextSubmit(query: String?) = true
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId != R.id.fake_switch_space) {
            return super.onOptionsItemSelected(item)
        }
        val spaces = viewModel.spaces.value
        MaterialAlertDialogBuilder(this)
                .setTitle(R.string.fake_switch_space)
                .setSingleChoiceItems(spaces.map { it.second }.toTypedArray(), spaces.indexOfFirst { it.first == userId }) { dialog, index ->
                    userId = spaces[index].first
                    loadAppList()
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        return true
    }


    companion object {
        fun start(context: Context) {
            val intent = Intent(context, FakeManagerActivity::class.java)
            context.startActivity(intent)
        }
    }
}