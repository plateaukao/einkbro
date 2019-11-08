package de.baumann.browser.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceFragmentCompat;

import java.util.ArrayList;
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

    private boolean showContributors;

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
                showContributors = false;
                showLicenseDialog(getString(R.string.license_title), getString(R.string.license_dialog));
                return false;
            }
        });
        Objects.requireNonNull(findPreference("settings_community")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                showContributors = true;
                showLicenseDialog(getString(R.string.setting_title_community), getString(R.string.cont_dialog));
                return false;
            }
        });
        Objects.requireNonNull(findPreference("settings_license")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                showContributors = false;
                showLicenseDialog(getString(R.string.license_title), getString(R.string.license_dialog));
                return false;
            }
        });
        Objects.requireNonNull(findPreference("settings_info")).setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                showContributors = false;
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

        if (showContributors) {
            dialog_text.append("\n\nGaukler Faun\n" +
                    "\u25AA Main developer and initiator of this project\n" +
                    "https://github.com/scoute-dich");

            dialog_text.append("\n\nAli Demirtas\n" +
                    "\u25AA Turkish Translation\n" +
                    "https://github.com/ali-demirtas");

            dialog_text.append("\n\nCGSLURP LLC\n" +
                    "\u25AA Russian translation\n" +
                    "https://crowdin.com/profile/gaich");

            dialog_text.append("\n\nDmitry Gaich\n" +
                    "\u25AA Helped to implement AdBlock and \"request desktop site\" in the previous version of \"FOSS Browser\".\n" +
                    "https://github.com/futrDevelopment");

            dialog_text.append("\n\nelement54\n" +
                    "\u25AA fix: keyboard problems (issue #105)\n" +
                    "\u25AA new: option to disable confirmation dialogs on exit\n" +
                    "https://github.com/element54");

            dialog_text.append("\n\nelmru\n" +
                    "\u25AA Taiwan Trad. Chinese Translation\n" +
                    "https://github.com/kogiokka");

            dialog_text.append("\n\nEnrico Monese\n" +
                    "\u25AA Italian Translation\n" +
                    "https://github.com/EnricoMonese");

            dialog_text.append("\n\nFrancois\n" +
                    "\u25AA French Translation\n" +
                    "https://github.com/franco27");

            dialog_text.append("\n\ngh-pmjm\n" +
                    "\u25AA Polish translation\n" +
                    "https://github.com/gh-pmjm");

            dialog_text.append("\n\ngr1sh\n" +
                    "\u25AA fix: some German strings (issues #124, #131)\n" +
                    "https://github.com/gr1sh");

            dialog_text.append("\n\nHarry Heights\n" +
                    "\u25AA Documentation of FOSS Browser\n" +
                    " https://github.com/HarryHeights");

            dialog_text.append("\n\nHeimen Stoffels\n" +
                    " \u25AA Dutch translation\n" +
                    "https://github.com/Vistaus");

            dialog_text.append("\n\nHellohat\n" +
                    "\u25AA French translation\n" +
                    "https://github.com/Hellohat");

            dialog_text.append("\n\nHerman Nunez\n" +
                    "\u25AA Spanish translation\n" +
                    "https://github.com/junior012");

            dialog_text.append("\n\nJumping Yang\n" +
                    "\u25AA Chinese translation in the previous version of \"FOSS Browser\"\n" +
                    "https://github.com/JumpingYang001");

            dialog_text.append("\n\nlishoujun\n" +
                    "\u25AA Chinese translation\n" +
                    "\u25AA bug hunting\n" +
                    "https://github.com/lishoujun");

            dialog_text.append("\n\nLukas Novotny\n" +
                    "\u25AA Czech translation\n" +
                    "https://crowdin.com/profile/novas78");

            dialog_text.append("\n\nOguz Ersen\n" +
                    "\u25AA Turkish translation\n" +
                    "https://crowdin.com/profile/oersen");

            dialog_text.append("\n\nPeter Bui\n" +
                    "\u25AA more font sizes to choose\n" +
                    "https://github.com/pbui");

            dialog_text.append("\n\nRodolfoCandidoB\n" +
                    "\u25AA Portuguese, Brazilian translation\n" +
                    "https://crowdin.com/profile/RodolfoCandidoB");

            dialog_text.append("\n\nSecangkir Kopi\n" +
                    "\u25AA Indonesian translation\n" +
                    "https://github.com/Secangkir-Kopi");

            dialog_text.append("\n\nSérgio Marques\n" +
                    "\u25AA Portuguese translation\n" +
                    "https://github.com/smarquespt");

            dialog_text.append("\n\nsplinet\n" +
                    "\u25AA Russian translation in the previous version of \"FOSS Browser\"\n" +
                    "https://github.com/splinet");

            dialog_text.append("\n\nSkewedZeppelin\n" +
                    "\u25AA Add option to enable Save-Data header\n" +
                    "https://github.com/SkewedZeppelin");

            dialog_text.append("\n\nTobiplayer\n" +
                    "\u25AA added Qwant search engine\n" +
                    "\u25AA option to open new tab instead of exiting\n" +
                    "https://github.com/Tobiplayer");

            dialog_text.append("\n\nVladimir Kosolapov\n" +
                    "\u25AA Russian translation\n" +
                    "https://github.com/0x264f");

            dialog_text.append("\n\nYC L\n" +
                    "\u25AA Chinese Translation\n" +
                    "https://github.com/smallg0at");
        }

        dialog_text.setMovementMethod(LinkMovementMethod.getInstance());
        dialog.setContentView(dialogView);
        dialog.show();
        HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
    }
}
