package top.niunaijun.blackboxa.util

import android.app.Activity
import android.os.Process

/** Closes the whole app process so engine settings are read again on the next start. */
fun Activity.closeApp() {
    finishAffinity()
    Process.killProcess(Process.myPid())
}
