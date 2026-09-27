package top.niunaijun.blackboxa.view.users

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.bean.AppInfo
import top.niunaijun.blackboxa.bean.UserBean
import top.niunaijun.blackboxa.data.AppsRepository
import top.niunaijun.blackboxa.databinding.ItemUserBinding

class UsersAdapter(
        private val onClick: (UserBean) -> Unit,
        private val onAppClick: (UserBean, AppInfo) -> Unit,
        private val onRename: (UserBean) -> Unit,
        private val onDuplicate: (UserBean) -> Unit,
        private val onDelete: (UserBean) -> Unit
) : ListAdapter<UserBean, UsersAdapter.UserVH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserVH {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserVH(binding)
    }

    override fun onBindViewHolder(holder: UserVH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserVH(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {

        private val previewViews =
                listOf(binding.preview1, binding.preview2, binding.preview3, binding.preview4, binding.preview5)

        fun bind(user: UserBean) {
            val resources = binding.root.resources
            binding.avatar.text = user.name.firstOrNull()?.uppercase() ?: user.id.toString()
            binding.name.text = user.name
            binding.appCount.text = resources.getQuantityString(R.plurals.user_app_count, user.appCount, user.appCount)

            val hasMore = user.appCount > AppsRepository.USER_PREVIEW_APP_COUNT
            val shownApps = if (hasMore) user.previewApps.dropLast(1) else user.previewApps
            previewViews.forEachIndexed { index, view ->
                val app = shownApps.getOrNull(index)
                view.visibility = if (app != null) View.VISIBLE else View.GONE
                view.setImageDrawable(app?.icon)
                view.contentDescription = app?.name
                view.setOnClickListener { app?.let { onAppClick(user, it) } }
            }
            binding.moreApps.visibility = if (hasMore) View.VISIBLE else View.GONE
            binding.moreApps.text = resources.getString(R.string.more_apps_count, user.appCount - shownApps.size)
            binding.moreApps.setOnClickListener { onClick(user) }
            binding.previews.visibility = if (user.previewApps.isEmpty()) View.GONE else View.VISIBLE

            binding.root.setOnClickListener { onClick(user) }
            binding.rename.setOnClickListener { onRename(user) }
            binding.duplicate.setOnClickListener { onDuplicate(user) }
            binding.delete.setOnClickListener { onDelete(user) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<UserBean>() {
            override fun areItemsTheSame(oldItem: UserBean, newItem: UserBean) = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: UserBean, newItem: UserBean) =
                    oldItem.name == newItem.name &&
                            oldItem.appCount == newItem.appCount &&
                            oldItem.previewApps.map { it.packageName } == newItem.previewApps.map { it.packageName }
        }
    }
}
