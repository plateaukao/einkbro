package de.baumann.browser.fragment;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import de.baumann.browser.Ninja.R;
import de.baumann.browser.util.Constants;
import de.baumann.browser.view.sortlistpreference.MultiSelectDragListPreference;
import de.baumann.browser.view.sortlistpreference.MultiSelectDragListPreferenceDialog;

public class Fragment_settings_UI extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_ui, rootKey);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments().getBoolean(Constants.ARG_LAUNCH_TOOLBAR_SETTING, false)) {
            showToolbarSettingDialog(null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
        sp.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // if ti's only for this preference, we should close it right after the dialog
        if (getArguments().getBoolean(Constants.ARG_LAUNCH_TOOLBAR_SETTING, false)) {
            getActivity().finish();
        }
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sp, String key) {
        if (key.equals("sp_exit") || key.equals("sp_toggle") || key.equals("sp_add") || key.equals("sp_theme")
                || key.equals("nav_position")  || key.equals("sp_hideOmni") || key.equals("start_tab") || key.equals("sp_hideSB")
                || key.equals("overView_place") || key.equals("overView_hide")) {
            sp.edit().putInt("restart_changed", 1).apply();
        }
    }

    private static final String DIALOG_FRAGMENT_TAG = "toolbar_icons";

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (getParentFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return;
        }

        if (preference instanceof MultiSelectDragListPreference) {
            showToolbarSettingDialog((MultiSelectDragListPreference)preference);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    public void showToolbarSettingDialog(MultiSelectDragListPreference preference) {
        MultiSelectDragListPreference localPreference;
        if (preference == null) {
            localPreference = findPreference("sp_toolbar_icons");
        } else {
            localPreference = preference;
        }
        boolean shouldFinishActivityWhenDismissed = preference == null;
        final DialogFragment f = new MultiSelectDragListPreferenceDialog(localPreference, shouldFinishActivityWhenDismissed);
        f.setTargetFragment(this, 0);
        f.show(getParentFragmentManager(), DIALOG_FRAGMENT_TAG);
    }
}
