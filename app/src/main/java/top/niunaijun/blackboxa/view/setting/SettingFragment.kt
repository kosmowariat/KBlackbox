package top.niunaijun.blackboxa.view.setting

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.snackbar.Snackbar
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.app.AppManager
import top.niunaijun.blackboxa.util.closeApp
import top.niunaijun.blackboxa.view.health.HealthActivity
import top.niunaijun.blackboxa.view.gms.GmsManagerActivity

class SettingFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.setting, rootKey)

        initThemeMode()

        initLanguage()

        initHealth()

        initGms()

        invalidHideState {
            val rootHidePreference: Preference = (findPreference("root_hide")!!)
            val hideRoot = AppManager.mBlackBoxLoader.hideRoot()
            rootHidePreference.setDefaultValue(hideRoot)
            rootHidePreference
        }

        invalidHideState {
            val daemonPreference: Preference = (findPreference("daemon_enable")!!)
            val mDaemonEnable = AppManager.mBlackBoxLoader.daemonEnable()
            daemonPreference.setDefaultValue(mDaemonEnable)
            daemonPreference
        }

        invalidHideState {
            val disableFlagSecurePreference: Preference = (findPreference("disable_flag_secure")!!)
            val mDisableFlagSecure = AppManager.mBlackBoxLoader.disableFlagSecure()
            disableFlagSecurePreference.setDefaultValue(mDisableFlagSecure)
            disableFlagSecurePreference
        }
    }

    private fun initThemeMode() {
        val themeModePreference: ListPreference = findPreference("theme_mode")!!
        themeModePreference.value = AppManager.mBlackBoxLoader.themeMode()
        themeModePreference.setOnPreferenceChangeListener { _, newValue ->
            AppManager.mBlackBoxLoader.invalidThemeMode(newValue as String)
            AppCompatDelegate.setDefaultNightMode(AppManager.mBlackBoxLoader.themeNightMode())
            true
        }
    }

    private fun initLanguage() {
        val languagePreference: ListPreference = findPreference("app_language")!!
        val values = resources.getStringArray(R.array.app_language_values)
        val currentTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        languagePreference.value =
                values.firstOrNull { it != SYSTEM_LANGUAGE && currentTags.equals(it, ignoreCase = true) }
                        ?: SYSTEM_LANGUAGE
        languagePreference.setOnPreferenceChangeListener { _, newValue ->
            val tags = if (newValue == SYSTEM_LANGUAGE) "" else newValue as String
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags))
            true
        }
    }

    private fun initHealth() {
        findPreference<Preference>("health")!!.setOnPreferenceClickListener {
            HealthActivity.start(requireContext())
            true
        }
    }

    private fun initGms() {
        val gmsManagerPreference: Preference = (findPreference("gms_manager")!!)

        if (BlackBoxCore.get().isSupportGms) {

            gmsManagerPreference.setOnPreferenceClickListener {
                GmsManagerActivity.start(requireContext())
                true
            }
        } else {
            gmsManagerPreference.summary = getString(R.string.no_gms)
            gmsManagerPreference.isEnabled = false
        }
    }

    private fun invalidHideState(block: () -> Preference) {
        val pref = block()
        pref.setOnPreferenceChangeListener { preference, newValue ->
            val tmpHide = (newValue == true)
            when (preference.key) {
                "root_hide" -> {

                    AppManager.mBlackBoxLoader.invalidHideRoot(tmpHide)
                }
                "daemon_enable" -> {
                    AppManager.mBlackBoxLoader.invalidDaemonEnable(tmpHide)
                }
                "disable_flag_secure" -> {
                    AppManager.mBlackBoxLoader.invalidDisableFlagSecure(tmpHide)
                }
            }

            Snackbar.make(requireView(), R.string.restart_module, Snackbar.LENGTH_LONG)
                    .setAction(R.string.close_app) { requireActivity().closeApp() }
                    .show()
            return@setOnPreferenceChangeListener true
        }
    }

    private companion object {
        const val SYSTEM_LANGUAGE = "system"
    }
}
