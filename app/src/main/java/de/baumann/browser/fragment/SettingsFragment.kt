package de.baumann.browser.fragment

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.baumann.browser.Ninja.BuildConfig
import de.baumann.browser.Ninja.R
import de.baumann.browser.activity.Settings_ClearActivity
import de.baumann.browser.unit.HelperUnit
import de.baumann.browser.view.dialog.PrinterDocumentPaperSizeDialog

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    private var showContributors = false
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_setting, rootKey)

        setupPreference("settings_data", DataSettingsFragment(), "data")
        setupPreference("settings_ui", UiSettingsFragment(), "ui")
        setupPreference("settings_font", FontSettingsFragment(), "font")
        setupPreference("settings_gesture", FragmentSettingsGesture(), "gesture")
        setupPreference("settings_start", StartSettingsFragment(), "start")

        findPreference<Preference>("settings_pdf_paper_size")?.setOnPreferenceClickListener {
            PrinterDocumentPaperSizeDialog(requireContext()).show()
            false
        }
        findPreference<Preference>("settings_clear")?.setOnPreferenceClickListener {
            requireActivity().startActivity(Intent(requireActivity(), Settings_ClearActivity::class.java))
            false
        }
        findPreference<Preference>("settings_license")?.setOnPreferenceClickListener {
            showContributors = false
            showLicenseDialog(getString(R.string.license_title), getString(R.string.license_dialog))
            false
        }
        findPreference<Preference>("settings_info")?.setOnPreferenceClickListener {
            showContributors = false
            showLicenseDialog(getString(R.string.menu_other_info), "v" + BuildConfig.VERSION_NAME.toString() + "<br><br>" + getString(R.string.changelog_dialog))
            false
        }
        findPreference<Preference>("settings_appSettings")?.setOnPreferenceClickListener {
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = Uri.fromParts("package", requireActivity().packageName, null)
            activity?.startActivity(intent)
            false
        }
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String) {
        if (key == "userAgent" || key == "sp_search_engine_custom" || key == "@string/sp_search_engine") {
            sp.edit().putInt("restart_changed", 1).apply()
        }
    }

    private fun setupPreference(prefName: String, fragment: PreferenceFragmentCompat, tag: String) {
        findPreference<Preference>(prefName)?.setOnPreferenceClickListener { _ ->
            parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.content_frame, fragment, "data")
                    .addToBackStack(null)
                    .commit()
            false
        }
    }

    private fun showLicenseDialog(title: String, text: String) {
        val dialog = BottomSheetDialog(requireActivity())
        val dialogView = View.inflate(requireActivity(), R.layout.dialog_text, null)
        dialogView.findViewById<TextView>(R.id.dialog_title).text = title
        dialogView.findViewById<TextView>(R.id.dialog_text).text = HelperUnit.textSpannable(text)
        if (showContributors) {
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""
    
    
    Gaukler Faun
    ▪ Main developer and initiator of this project
    https://github.com/scoute-dich
    """.trimIndent())
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""
    
    
    Ali Demirtas
    ▪ Turkish Translation
    https://github.com/ali-demirtas
    """.trimIndent())
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""
    
    
    CGSLURP LLC
    ▪ Russian translation
    https://crowdin.com/profile/gaich
    """.trimIndent())
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""

Dmitry Gaich
▪ Helped to implement AdBlock and "request desktop site" in the previous version of "EinkBro Browser".
https://github.com/futrDevelopment""")
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""
    
    
    element54
    ▪ fix: keyboard problems (issue #105)
    ▪ new: option to disable confirmation dialogs on exit
    https://github.com/element54
    """.trimIndent())
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""
    
    
    elmru
    ▪ Taiwan Trad. Chinese Translation
    https://github.com/kogiokka
    """.trimIndent())
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""
    
    
    Enrico Monese
    ▪ Italian Translation
    https://github.com/EnricoMonese
    """.trimIndent())
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""
    
    
    Francois
    ▪ French Translation
    https://github.com/franco27
    """.trimIndent())
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""
    
    
    gh-pmjm
    ▪ Polish translation
    https://github.com/gh-pmjm
    """.trimIndent())
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""
    
    
    gr1sh
    ▪ fix: some German strings (issues #124, #131)
    https://github.com/gr1sh
    """.trimIndent())
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""

Harry Heights
▪ Documentation of EinkBro Browser
 https://github.com/HarryHeights""")
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""

Heimen Stoffels
 ▪ Dutch translation
https://github.com/Vistaus""")
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""
    
    
    Hellohat
    ▪ French translation
    https://github.com/Hellohat
    """.trimIndent())
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""
    
    
    Herman Nunez
    ▪ Spanish translation
    https://github.com/junior012
    """.trimIndent())
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""
    
    
    Jumping Yang
    ▪ Chinese translation in the previous version of "EinkBro Browser"
    https://github.com/JumpingYang001
    """.trimIndent())
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""
    
    
    lishoujun
    ▪ Chinese translation
    ▪ bug hunting
    https://github.com/lishoujun
    """.trimIndent())
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""
    
    
    Lukas Novotny
    ▪ Czech translation
    https://crowdin.com/profile/novas78
    """.trimIndent())
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""
    
    
    Oguz Ersen
    ▪ Turkish translation
    https://crowdin.com/profile/oersen
    """.trimIndent())
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""
    
    
    Peter Bui
    ▪ more font sizes to choose
    https://github.com/pbui
    """.trimIndent())
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""
    
    
    RodolfoCandidoB
    ▪ Portuguese, Brazilian translation
    https://crowdin.com/profile/RodolfoCandidoB
    """.trimIndent())
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""
    
    
    Secangkir Kopi
    ▪ Indonesian translation
    https://github.com/Secangkir-Kopi
    """.trimIndent())
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""
    
    
    Sérgio Marques
    ▪ Portuguese translation
    https://github.com/smarquespt
    """.trimIndent())
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""
    
    
    splinet
    ▪ Russian translation in the previous version of "EinkBro Browser"
    https://github.com/splinet
    """.trimIndent())
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""
    
    
    SkewedZeppelin
    ▪ Add option to enable Save-Data header
    https://github.com/SkewedZeppelin
    """.trimIndent())
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""
    
    
    Tobiplayer
    ▪ added Qwant search engine
    ▪ option to open new tab instead of exiting
    https://github.com/Tobiplayer
    """.trimIndent())
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""
    
    
    Vladimir Kosolapov
    ▪ Russian translation
    https://github.com/0x264f
    """.trimIndent())
            dialogView.findViewById<TextView>(R.id.dialog_text).append("""
    
    
    YC L
    ▪ Chinese Translation
    https://github.com/smallg0at
    """.trimIndent())
        }
        dialogView.findViewById<TextView>(R.id.dialog_text).movementMethod = LinkMovementMethod.getInstance()
        dialog.setContentView(dialogView)
        dialog.show()
        HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
    }
}