package de.baumann.browser.fragment

import androidx.preference.PreferenceFragmentCompat
import android.os.Bundle
import de.baumann.browser.Ninja.R

class SearchSettingsFragment : PreferenceFragmentCompat(), FragmentTitleInterface {
    override fun getTitleId() = R.string.setting_title_search

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_search, rootKey)
    }
}