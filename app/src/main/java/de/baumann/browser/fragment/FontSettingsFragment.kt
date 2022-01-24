package de.baumann.browser.fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import de.baumann.browser.Ninja.R
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.preference.CustomFontInfo
import de.baumann.browser.util.Constants
import de.baumann.browser.view.dialog.DialogManager
import de.baumann.browser.view.dialog.ToolbarConfigDialog
import de.baumann.browser.view.dialog.TouchAreaDialog
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import java.io.File

class FontSettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener, KoinComponent {
    private val config: ConfigManager by inject()
    private val dialogManager: DialogManager by lazy { DialogManager(requireActivity()) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_font, rootKey)

        findPreference<Preference>(ConfigManager.K_CUSTOM_FONT)?.apply {
            summary = config.customFontInfo?.name ?: "not configured"
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                openFontFilePicker()
                true
            }
        }
        findPreference<Preference>("settings_font_type")?.apply {
            summary = getString(config.fontType.resId)
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                dialogManager.showFontTypeDialog()
                true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FONT_PICKER_REQUEST_CODE && resultCode == ComponentActivity.RESULT_OK) {
            val uri = data?.data ?: return
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context?.contentResolver?.takePersistableUriPermission(uri, takeFlags)

            val file = File(uri.path)
            config.customFontInfo = CustomFontInfo(file.name, uri.toString())

            return
        }
    }

    private fun openFontFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = Constants.MIME_TYPE_ANY
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivityForResult(intent, FONT_PICKER_REQUEST_CODE)
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String) {
        when (key) {
            ConfigManager.K_CUSTOM_FONT ->
                findPreference<Preference>(ConfigManager.K_CUSTOM_FONT)?.apply {
                    summary = config.customFontInfo?.name ?: "not configured"
                }
            ConfigManager.K_FONT_TYPE ->
                findPreference<Preference>("settings_font_type")?.apply {
                    summary = getString(config.fontType.resId)
                }
        }
    }

    companion object {
        const val FONT_PICKER_REQUEST_CODE = 4
    }
}