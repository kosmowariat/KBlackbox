package top.niunaijun.blackboxa.view.users

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.bean.AppInfo
import top.niunaijun.blackboxa.bean.UserBean
import top.niunaijun.blackboxa.databinding.ItemUserBinding
import top.niunaijun.blackboxa.databinding.ItemUserGridBinding

class UsersAdapter(
        private val onClick: (UserBean) -> Unit,
        private val onAppClick: (UserBean, AppInfo) -> Unit,
        private val onRename: (UserBean) -> Unit,
        private val onDuplicate: (UserBean) -> Unit,
        private val onDelete: (UserBean) -> Unit
) : ListAdapter<UserBean, UsersAdapter.UserVH>(DIFF) {

    var gridMode = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun getItemViewType(position: Int) = if (gridMode) VIEW_TYPE_GRID else VIEW_TYPE_LIST

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserVH {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_GRID) {
            val binding = ItemUserGridBinding.inflate(inflater, parent, false)
            UserVH(
                    binding.root, null, binding.name, null, binding.previews,
                    listOf(binding.preview1, binding.preview2, binding.preview3), binding.moreApps,
                    binding.emptyHint, null, null, null
            )
        } else {
            val binding = ItemUserBinding.inflate(inflater, parent, false)
            UserVH(
                    binding.root, binding.avatar, binding.name, binding.appCount, binding.previews,
                    listOf(binding.preview1, binding.preview2, binding.preview3, binding.preview4, binding.preview5),
                    binding.moreApps, binding.emptyHint, binding.rename, binding.duplicate, binding.delete
            )
        }
    }

    override fun onBindViewHolder(holder: UserVH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserVH(
            root: View,
            private val avatar: TextView?,
            private val name: TextView,
            private val appCount: TextView?,
            private val previews: View,
            private val previewViews: List<ImageButton>,
            private val moreApps: TextView,
            private val emptyHint: View,
            private val rename: View?,
            private val duplicate: View?,
            private val delete: View?
    ) : RecyclerView.ViewHolder(root) {

        fun bind(user: UserBean) {
            val resources = itemView.resources
            avatar?.text = user.name.firstOrNull()?.uppercase() ?: user.id.toString()
            name.text = user.name
            appCount?.text = resources.getQuantityString(R.plurals.user_app_count, user.appCount, user.appCount)

            val hasMore = user.appCount > previewViews.size
            val shownApps = user.previewApps.take(if (hasMore) previewViews.size - 1 else previewViews.size)
            previewViews.forEachIndexed { index, view ->
                val app = shownApps.getOrNull(index)
                view.visibility = if (app != null) View.VISIBLE else View.GONE
                view.setImageDrawable(app?.icon)
                view.contentDescription = app?.name
                view.setOnClickListener { app?.let { onAppClick(user, it) } }
            }
            moreApps.visibility = if (hasMore) View.VISIBLE else View.GONE
            moreApps.text = resources.getString(R.string.more_apps_count, user.appCount - shownApps.size)
            moreApps.setOnClickListener { onClick(user) }
            previews.visibility = if (user.previewApps.isEmpty()) View.GONE else View.VISIBLE
            emptyHint.visibility = if (user.previewApps.isEmpty()) View.VISIBLE else View.GONE

            itemView.setOnClickListener { onClick(user) }
            rename?.setOnClickListener { onRename(user) }
            duplicate?.setOnClickListener { onDuplicate(user) }
            delete?.setOnClickListener { onDelete(user) }
            itemView.setOnLongClickListener(if (rename == null) View.OnLongClickListener { showMenu(user) } else null)
        }

        private fun showMenu(user: UserBean): Boolean {
            PopupMenu(itemView.context, itemView).apply {
                menuInflater.inflate(R.menu.menu_user_card, menu)
                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.user_rename -> onRename(user)
                        R.id.user_duplicate -> onDuplicate(user)
                        R.id.user_delete -> onDelete(user)
                    }
                    true
                }
                show()
            }
            return true
        }
    }

    companion object {
        private const val VIEW_TYPE_LIST = 0
        private const val VIEW_TYPE_GRID = 1

        private val DIFF = object : DiffUtil.ItemCallback<UserBean>() {
            override fun areItemsTheSame(oldItem: UserBean, newItem: UserBean) = oldItem.id == newItem.id

            // Compares the package names, not the AppInfo instances, which have no equals().
            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: UserBean, newItem: UserBean) =
                    oldItem.name == newItem.name &&
                            oldItem.appCount == newItem.appCount &&
                            oldItem.previewApps.map { it.packageName } == newItem.previewApps.map { it.packageName }
        }
    }
}
