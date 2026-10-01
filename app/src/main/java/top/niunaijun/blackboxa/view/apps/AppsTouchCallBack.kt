package top.niunaijun.blackboxa.view.apps

import android.graphics.Canvas
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

/**
 * Long-press starts a drag. When the finger is released without the app having been moved,
 * [onLongPressRelease] is called so the screen can show the app's context menu instead.
 */
class AppsTouchCallBack(
    private val onMoveBlock: (from: Int, to: Int) -> Unit,
    private val onLongPressRelease: (view: View, position: Int) -> Unit
) : ItemTouchHelper.Callback() {

    private var reordered = false
    private var travelled = 0f

    companion object {
        private const val TAG = "AppsTouchCallBack"
        private const val DRAG_ALPHA = 0.8f
        private const val LONG_PRESS_SLOP_PX = 40f
    }

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        return makeMovementFlags(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
            0
        )
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPosition = viewHolder.bindingAdapterPosition
        val toPosition = target.bindingAdapterPosition
        if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
            Log.w(TAG, "Invalid positions: from=$fromPosition, to=$toPosition")
            return false
        }
        if (fromPosition == toPosition) {
            return false
        }
        onMoveBlock(fromPosition, toPosition)
        reordered = true
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        when (actionState) {
            ItemTouchHelper.ACTION_STATE_DRAG -> viewHolder?.itemView?.alpha = DRAG_ALPHA
            ItemTouchHelper.ACTION_STATE_IDLE -> viewHolder?.itemView?.alpha = 1.0f
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        if (isCurrentlyActive && actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            travelled = maxOf(travelled, kotlin.math.hypot(dX, dY))
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        viewHolder.itemView.alpha = 1.0f
        val position = viewHolder.bindingAdapterPosition
        if (!reordered && travelled < LONG_PRESS_SLOP_PX && position != RecyclerView.NO_POSITION) {
            onLongPressRelease(viewHolder.itemView, position)
        }
        reordered = false
        travelled = 0f
    }

    override fun canDropOver(
        recyclerView: RecyclerView,
        current: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return target.bindingAdapterPosition != RecyclerView.NO_POSITION
    }
}
