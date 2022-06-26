package de.baumann.browser.fragment

import androidx.preference.PreferenceFragmentCompat
import de.baumann.browser.fragment.FragmentTitleInterface
import android.os.Bundle
import de.baumann.browser.Ninja.R
import android.content.Intent
import androidx.preference.Preference
import de.baumann.browser.activity.Whitelist_AdBlock
import de.baumann.browser.activity.Whitelist_Javascript
import de.baumann.browser.activity.Whitelist_Cookie

class StartSettingsFragment : PreferenceFragmentCompat(), FragmentTitleInterface {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_start, rootKey)
        findPreference<Preference>("start_AdBlock")!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { preference: Preference? ->
                val intent = Intent(activity, Whitelist_AdBlock::class.java)
                requireActivity().startActivity(intent)
                false
            }
        findPreference<Preference>("start_java")!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { preference: Preference? ->
                val intent = Intent(activity, Whitelist_Javascript::class.java)
                requireActivity().startActivity(intent)
                false
            }
        findPreference<Preference>("start_cookie")!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { preference: Preference? ->
                val intent = Intent(activity, Whitelist_Cookie::class.java)
                requireActivity().startActivity(intent)
                false
            }
    }

    override fun getTitleId(): Int = R.string.setting_title_start_control
}