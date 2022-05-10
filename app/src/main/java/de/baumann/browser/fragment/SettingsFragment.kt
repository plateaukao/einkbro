package de.baumann.browser.fragment

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Layout
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.method.MovementMethod
import android.text.style.URLSpan
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.baumann.browser.Ninja.BuildConfig
import de.baumann.browser.Ninja.R
import de.baumann.browser.activity.BrowserActivity
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
            showContributors = true
            showLicenseDialog(getString(R.string.license_title), getString(R.string.license_dialog))
            false
        }
        findPreference<Preference>("settings_info")?.setOnPreferenceClickListener {
            showContributors = false
            showLicenseDialog(getString(R.string.menu_other_info), getString(R.string.changelog_dialog))
            false
        }
        findPreference<Preference>("settings_appSettings")?.setOnPreferenceClickListener {
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = Uri.fromParts("package", requireActivity().packageName, null)
            activity?.startActivity(intent)
            false
        }
        findPreference<Preference>("settings_version")?.title = "Daniel Kao, v" + BuildConfig.VERSION_NAME.toString()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String) {
        if (key == "userAgent" || key == "sp_search_engine_custom" || key == "@string/sp_search_engine") {
            sp.edit().putBoolean("restart_changed", true).apply()
        }
    }

    private fun setupPreference(prefName: String, fragment: PreferenceFragmentCompat, tag: String) {
        findPreference<Preference>(prefName)?.setOnPreferenceClickListener { pref ->
            parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.content_frame, fragment, "data")
                    .commit()
            false
        }
    }

    private fun showLicenseDialog(title: String, text: String) {
        val dialogView = View.inflate(requireActivity(), R.layout.dialog_text, null).apply {
            findViewById<TextView>(R.id.dialog_title).text = title
            findViewById<TextView>(R.id.dialog_text).text = HelperUnit.textSpannable(text)
            findViewById<TextView>(R.id.dialog_text).movementMethod = LinkMovementMethod.getInstance()
        }
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
        val dialog = BottomSheetDialog(requireActivity()).apply {
            setContentView(dialogView)
            show()
        }
        HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
    }
}

class MyLinkMovementMethod() : LinkMovementMethod() {
    override fun onTouchEvent(textView: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        // Get the event action
        var action = event.action

        // If action has finished
        if (action == MotionEvent.ACTION_UP) {
            // Locate the area that was pressed
            var x = event.x.toInt()
            var y = event.y.toInt()
            x -= textView.totalPaddingLeft
            y -= textView.totalPaddingTop
            x += textView.scrollX
            y += textView.scrollY

            // Locate the URL text
            val layout: Layout = textView.layout
            val line: Int = layout.getLineForVertical(y)
            val off: Int = layout.getOffsetForHorizontal(line, x.toFloat())

            // Find the URL that was pressed
            val link = buffer.getSpans(off, off, URLSpan::class.java)
            // If we've found a URL
            if (link.isNotEmpty()) {
                // Find the URL
                val url = link[0].url
                // If it's a valid URL
                if (url.contains("https") or
                        url.contains("tel") or
                        url.contains("mailto") or
                        url.contains("http") or
                        url.contains("https") or
                        url.contains("www")
                ) {
                    val intent = Intent(textView.context, BrowserActivity::class.java).apply {
                        data = Uri.parse(url)
                    }
                    intent.action = ACTION_VIEW
                    textView.context.startActivity(intent)
                }
                return true
            }
        }
        return super.onTouchEvent(textView, buffer, event)
    }

    companion object {
        // A new LinkMovementMethod
        private val myLinkMovementMethod = MyLinkMovementMethod()
        fun getInstance(): MovementMethod {
            return myLinkMovementMethod
        }
    }
}