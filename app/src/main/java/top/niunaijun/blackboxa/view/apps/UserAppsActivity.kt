package top.niunaijun.blackboxa.view.apps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.databinding.ActivityUserAppsBinding
import top.niunaijun.blackboxa.util.Resolution
import top.niunaijun.blackboxa.util.inflate
import top.niunaijun.blackboxa.view.base.LoadingActivity
import top.niunaijun.blackboxa.view.list.ListActivity

class UserAppsActivity : LoadingActivity() {

    private val viewBinding: ActivityUserAppsBinding by inflate()

    private val userId by lazy { intent.getIntExtra(EXTRA_USER_ID, 0) }

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
        viewBinding.toolbarLayout.toolbar.title = intent.getStringExtra(EXTRA_USER_NAME)

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
