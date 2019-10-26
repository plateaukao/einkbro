package de.baumann.browser.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;

import de.baumann.browser.activity.Settings_ClearActivity;
import de.baumann.browser.activity.Settings_DataActivity;
import de.baumann.browser.activity.Settings_FilterActivity;
import de.baumann.browser.activity.Settings_GestureActivity;
import de.baumann.browser.activity.Settings_StartActivity;
import de.baumann.browser.activity.Settings_UIActivity;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.Ninja.R;

public class Fragment_settings extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_setting, rootKey);

        Objects.requireNonNull(findPreference("settings_filter")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                Intent intent = new Intent(getActivity(), Settings_FilterActivity.class);
                Objects.requireNonNull(getActivity()).startActivity(intent);
                return false;
            }
        });
        Objects.requireNonNull(findPreference("settings_data")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                Intent intent = new Intent(getActivity(), Settings_DataActivity.class);
                Objects.requireNonNull(getActivity()).startActivity(intent);
                return false;
            }
        });
        Objects.requireNonNull(findPreference("settings_ui")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                Intent intent = new Intent(getActivity(), Settings_UIActivity.class);
                Objects.requireNonNull(getActivity()).startActivity(intent);
                return false;
            }
        });
        Objects.requireNonNull(findPreference("settings_gesture")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                Intent intent = new Intent(getActivity(), Settings_GestureActivity.class);
                Objects.requireNonNull(getActivity()).startActivity(intent);
                return false;
            }
        });
        Objects.requireNonNull(findPreference("settings_start")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                Intent intent = new Intent(getActivity(), Settings_StartActivity.class);
                Objects.requireNonNull(getActivity()).startActivity(intent);
                return false;
            }
        });
        Objects.requireNonNull(findPreference("settings_clear")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                Intent intent = new Intent(getActivity(), Settings_ClearActivity.class);
                Objects.requireNonNull(getActivity()).startActivity(intent);
                return false;
            }
        });
        Objects.requireNonNull(findPreference("settings_license")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                showLicenseDialog(getString(R.string.license_title), getString(R.string.license_dialog));
                return false;
            }
        });
        Objects.requireNonNull(findPreference("settings_community")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                showLicenseDialog(getString(R.string.setting_title_community), getString(R.string.cont_dialog));
                return false;
            }
        });
        Objects.requireNonNull(findPreference("settings_license")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                showLicenseDialog(getString(R.string.license_title), getString(R.string.license_dialog));
                return false;
            }
        });
        Objects.requireNonNull(findPreference("settings_info")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                showLicenseDialog(getString(R.string.menu_other_info), getString(R.string.changelog_dialog));
                return false;
            }
        });
        Objects.requireNonNull(findPreference("settings_help")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                HelperUnit.show_dialogHelp(getActivity());
                return false;
            }
        });
        Objects.requireNonNull(findPreference("settings_appSettings")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", Objects.requireNonNull(getActivity()).getPackageName(), null);
                intent.setData(uri);
                getActivity().startActivity(intent);
                return false;
            }
        });
    }


    private void showLicenseDialog(String title, String text) {

        final BottomSheetDialog dialog = new BottomSheetDialog(Objects.requireNonNull(getActivity()));
        View dialogView = View.inflate(getActivity(), R.layout.dialog_text, null);

        TextView dialog_title = dialogView.findViewById(R.id.dialog_title);
        dialog_title.setText(title);

        TextView dialog_text = dialogView.findViewById(R.id.dialog_text);
        dialog_text.setText(HelperUnit.textSpannable(text));
        dialog_text.setMovementMethod(LinkMovementMethod.getInstance());

        dialog.setContentView(dialogView);
        dialog.show();
        HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
    }
}
