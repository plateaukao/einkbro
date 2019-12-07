package de.baumann.browser.fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.Objects;

import de.baumann.browser.Ninja.R;
import de.baumann.browser.unit.HelperUnit;

public class Fragment_settings_Filter extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_filter, rootKey);
        final SharedPreferences sp = getPreferenceScreen().getSharedPreferences();

        final Preference filter_01 = findPreference("filter_01");
        Objects.requireNonNull(filter_01).setTitle(sp.getString("icon_01", Objects.requireNonNull(getActivity()).getResources().getString(R.string.color_red)));
        final Preference filter_02 = findPreference("filter_02");
        Objects.requireNonNull(filter_02).setTitle(sp.getString("icon_02", Objects.requireNonNull(getActivity()).getResources().getString(R.string.color_pink)));
        final Preference filter_03 = findPreference("filter_03");
        Objects.requireNonNull(filter_03).setTitle(sp.getString("icon_03", Objects.requireNonNull(getActivity()).getResources().getString(R.string.color_purple)));
        final Preference filter_04 = findPreference("filter_04");
        Objects.requireNonNull(filter_04).setTitle(sp.getString("icon_04", Objects.requireNonNull(getActivity()).getResources().getString(R.string.color_blue)));
        final Preference filter_05 = findPreference("filter_05");
        Objects.requireNonNull(filter_05).setTitle(sp.getString("icon_05", Objects.requireNonNull(getActivity()).getResources().getString(R.string.color_teal)));
        final Preference filter_06 = findPreference("filter_06");
        Objects.requireNonNull(filter_06).setTitle(sp.getString("icon_06", Objects.requireNonNull(getActivity()).getResources().getString(R.string.color_green)));
        final Preference filter_07 = findPreference("filter_07");
        Objects.requireNonNull(filter_07).setTitle(sp.getString("icon_07", Objects.requireNonNull(getActivity()).getResources().getString(R.string.color_lime)));
        final Preference filter_08 = findPreference("filter_08");
        Objects.requireNonNull(filter_08).setTitle(sp.getString("icon_08", Objects.requireNonNull(getActivity()).getResources().getString(R.string.color_yellow)));
        final Preference filter_09 = findPreference("filter_09");
        Objects.requireNonNull(filter_09).setTitle(sp.getString("icon_09", Objects.requireNonNull(getActivity()).getResources().getString(R.string.color_orange)));
        final Preference filter_10 = findPreference("filter_10");
        Objects.requireNonNull(filter_10).setTitle(sp.getString("icon_10", Objects.requireNonNull(getActivity()).getResources().getString(R.string.color_brown)));
        final Preference filter_11 = findPreference("filter_11");
        Objects.requireNonNull(filter_11).setTitle(sp.getString("icon_11", Objects.requireNonNull(getActivity()).getResources().getString(R.string.color_grey)));

        filter_01.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!sp.getBoolean("filter_01", true)) {
                    editFilterNames("icon_01", getString(R.string.color_red), filter_01);
                    return true;
                }
                return true;
            }
        });
        filter_02.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!sp.getBoolean("filter_02", true)) {
                    editFilterNames("icon_02", getString(R.string.color_pink), filter_02);
                    return true;
                }
                return true;
            }
        });
        filter_03.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!sp.getBoolean("filter_03", true)) {
                    editFilterNames("icon_03", getString(R.string.color_purple), filter_03);
                    return true;
                }
                return true;
            }
        });
        filter_04.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!sp.getBoolean("filter_04", true)) {
                    editFilterNames("icon_04", getString(R.string.color_blue), filter_04);
                    return true;
                }
                return true;
            }
        });
        filter_05.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!sp.getBoolean("filter_05", true)) {
                    editFilterNames("icon_05", getString(R.string.color_teal), filter_05);
                    return true;
                }
                return true;
            }
        });
        filter_06.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!sp.getBoolean("filter_06", true)) {
                    editFilterNames("icon_06", getString(R.string.color_green), filter_06);
                    return true;
                }
                return true;
            }
        });
        filter_07.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!sp.getBoolean("filter_07", true)) {
                    editFilterNames("icon_07", getString(R.string.color_lime), filter_07);
                    return true;
                }
                return true;
            }
        });
        filter_08.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!sp.getBoolean("filter_08", true)) {
                    editFilterNames("icon_08", getString(R.string.color_yellow), filter_08);
                    return true;
                }
                return true;
            }
        });
        filter_09.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!sp.getBoolean("filter_09", true)) {
                    editFilterNames("icon_09", getString(R.string.color_orange), filter_09);
                    return true;
                }
                return true;
            }
        });
        filter_10.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!sp.getBoolean("filter_10", true)) {
                    editFilterNames("icon_10", getString(R.string.color_brown), filter_10);
                    return true;
                }
                return true;
            }
        });
        filter_11.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!sp.getBoolean("filter_11", true)) {
                    editFilterNames("icon_11", getString(R.string.color_grey), filter_11);
                    return true;
                }
                return true;
            }
        });
    }

    private void editFilterNames (final String string_spName_icon_01, final String string_spNameDefault, final Preference preference) {

        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(Objects.requireNonNull(getActivity()));
        View dialogView = View.inflate(getActivity(), R.layout.dialog_edit_title, null);

        final EditText editText = dialogView.findViewById(R.id.dialog_edit);
        final SharedPreferences sp = getPreferenceScreen().getSharedPreferences();

        editText.setHint(R.string.dialog_title_hint);
        editText.setText(sp.getString(string_spName_icon_01, string_spNameDefault));

        Button action_ok = dialogView.findViewById(R.id.action_ok);
        action_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = editText.getText().toString().trim();
                sp.edit().putString(string_spName_icon_01, text).apply();
                Objects.requireNonNull(preference).setTitle(sp.getString(string_spName_icon_01, string_spNameDefault));
                bottomSheetDialog.cancel();
            }
        });
        Button action_cancel = dialogView.findViewById(R.id.action_cancel);
        action_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetDialog.cancel();
            }
        });
        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.show();
        HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
    }
}
