package top.niunaijun.blackboxa.view.users

import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.bean.DuplicateUserBean
import top.niunaijun.blackboxa.bean.UserBean
import top.niunaijun.blackboxa.databinding.DialogDuplicateUserBinding
import top.niunaijun.blackboxa.databinding.DialogTextInputBinding
import top.niunaijun.blackboxa.view.base.LoadingActivity

class UserDialogs(private val activity: LoadingActivity, private val viewModel: UsersViewModel) {

    fun showCreateDialog() {
        showNameDialog(R.string.new_user, "", viewModel.isGmsSupported) { name, installGms ->
            if (installGms) {
                activity.showLoading()
            }
            viewModel.createUser(name, installGms)
        }
    }

    fun showRenameDialog(user: UserBean, onRenamed: (String) -> Unit = {}) {
        showNameDialog(R.string.rename_user, user.name) { name, _ ->
            viewModel.renameUser(user.id, name)
            onRenamed(name)
        }
    }

    fun showDuplicateDialog(request: DuplicateUserBean) {
        val binding = DialogDuplicateUserBinding.inflate(LayoutInflater.from(activity))
        val defaultName = activity.getString(R.string.duplicate_user_default_name, request.source.name)
        binding.input.setText(defaultName)
        binding.input.setSelection(defaultName.length)
        val resources = activity.resources
        val iconSize = resources.getDimensionPixelSize(R.dimen.preview_app_icon_size)
        val checkBoxes = request.apps.map { app ->
            MaterialCheckBox(activity).apply {
                text = app.name
                compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.spacing_small)
                app.icon?.mutate()?.let { icon ->
                    icon.setBounds(0, 0, iconSize, iconSize)
                    setCompoundDrawablesRelative(null, null, icon, null)
                }
                binding.appsContainer.addView(this)
            } to app.packageName
        }
        val hasApps = request.apps.isNotEmpty()
        binding.copyDataHeader.visibility = if (hasApps) View.VISIBLE else View.GONE
        binding.copyDataHint.visibility = if (hasApps) View.VISIBLE else View.GONE

        val dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.duplicate_user_title)
                .setView(binding.root)
                .setPositiveButton(R.string.duplicate_user) { _, _ ->
                    val copyDataFor = checkBoxes.filter { it.first.isChecked }.map { it.second }.toSet()
                    activity.showLoading()
                    viewModel.duplicateUser(request.source.id, binding.input.text.toString().trim(), copyDataFor)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        val duplicateButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        binding.input.doAfterTextChanged { duplicateButton.isEnabled = !it.isNullOrBlank() }
    }

    fun showDeleteDialog(user: UserBean) {
        MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.delete_user)
                .setMessage(activity.getString(R.string.delete_user_hint, user.name))
                .setPositiveButton(R.string.delete_user) { _, _ -> viewModel.deleteUser(user.id) }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    private fun showNameDialog(
            @StringRes title: Int,
            initialName: String,
            showGmsOption: Boolean = false,
            onConfirm: (name: String, installGms: Boolean) -> Unit
    ) {
        val binding = DialogTextInputBinding.inflate(LayoutInflater.from(activity))
        binding.inputLayout.hint = activity.getString(R.string.user_name)
        binding.input.setText(initialName)
        binding.input.setSelection(initialName.length)
        binding.installGms.visibility = if (showGmsOption) View.VISIBLE else View.GONE
        val dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setView(binding.root)
                .setPositiveButton(R.string.done) { _, _ ->
                    onConfirm(binding.input.text.toString().trim(), showGmsOption && binding.installGms.isChecked)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        val doneButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        doneButton.isEnabled = initialName.isNotBlank()
        binding.input.doAfterTextChanged { doneButton.isEnabled = !it.isNullOrBlank() }
        binding.input.requestFocus()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }
}
