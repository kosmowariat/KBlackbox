package top.niunaijun.blackboxa.util

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import java.util.Collections

/**
 * Minimal ViewBinding list adapter with DiffUtil updates. The list is mutable so items can be
 * updated in place ([replaceAt]) and reordered by drag ([moveItem]).
 */
class BindingAdapter<T : Any, VB : ViewBinding>(
        private val inflateBinding: (LayoutInflater, ViewGroup, Boolean) -> VB,
        private val itemId: (T) -> Any,
        private val bindItem: (binding: VB, item: T) -> Unit
) : RecyclerView.Adapter<BindingAdapter.Holder<VB>>() {

    class Holder<VB : ViewBinding>(val binding: VB) : RecyclerView.ViewHolder(binding.root)

    private val items = mutableListOf<T>()

    var onItemClick: ((view: View, item: T, position: Int) -> Unit)? = null

    var onItemLongClick: ((view: View, item: T, position: Int) -> Unit)? = null

    fun getItems(): MutableList<T> = items

    fun setItems(newItems: List<T>) {
        val oldItems = items.toList()
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = oldItems.size

            override fun getNewListSize() = newItems.size

            override fun areItemsTheSame(oldPosition: Int, newPosition: Int) =
                    itemId(oldItems[oldPosition]) == itemId(newItems[newPosition])

            override fun areContentsTheSame(oldPosition: Int, newPosition: Int) =
                    oldItems[oldPosition] == newItems[newPosition]
        })
        items.clear()
        items.addAll(newItems)
        diff.dispatchUpdatesTo(this)
    }

    fun replaceAt(position: Int, item: T) {
        items[position] = item
        notifyItemChanged(position)
    }

    fun moveItem(from: Int, to: Int) {
        if (from < to) {
            for (i in from until to) Collections.swap(items, i, i + 1)
        } else {
            for (i in from downTo to + 1) Collections.swap(items, i, i - 1)
        }
        notifyItemMoved(from, to)
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder<VB> {
        val holder = Holder(inflateBinding(LayoutInflater.from(parent.context), parent, false))
        holder.itemView.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onItemClick?.invoke(it, items[position], position)
            }
        }
        holder.itemView.setOnLongClickListener {
            val position = holder.bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION || onItemLongClick == null) {
                false
            } else {
                onItemLongClick?.invoke(it, items[position], position)
                true
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: Holder<VB>, position: Int) {
        bindItem(holder.binding, items[position])
    }
}
