package top.niunaijun.blackboxa.view.base

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

open class BaseViewModel : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Unhandled error in a ViewModel coroutine", throwable)
    }

    /** Runs [block] in the ViewModel scope; repositories switch to the right dispatcher themselves. */
    protected fun launch(block: suspend () -> Unit): Job = viewModelScope.launch(exceptionHandler) { block() }

    private companion object {
        const val TAG = "BaseViewModel"
    }
}
