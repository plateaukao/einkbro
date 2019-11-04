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

        dialog_text.append("\n\nGaukler Faun\n" +
                "\u25AA Main developer and initiator of this project\n" +
                "https://github.com/scoute-dich");

        dialog_text.append("\n\nAli Demirtas\n" +
                "\u25AA Turkish Translation\n" +
                "https://github.com/ali-demirtas");

        dialog_text.append("\n\nCGSLURP LLC\n" +
                "\u25AA Helped to implement AdBlock and \"request desktop site\" in the previous version of \"FOSS Browser\".\n" +
                "https://github.com/futrDevelopment");

        dialog_text.append("\n\nelement54\n" +
                "\u25AA fix: keyboard problems (issue #105)&lt;br>\n" +
                "\u25AA new: option to disable confirmation dialogs on exit\n" +
                "https://github.com/element54");

        dialog_text.append("\n\nelmru\n" +
                "\u25AA Taiwan Trad. Chinese Translation\n" +
                "https://github.com/kogiokka");

        /*

        &lt;b>Enrico Monese&lt;/b>&lt;br>
        \u25AA Italian Translation&lt;br>
                &lt;i>https://github.com/EnricoMonese&lt;/i>&lt;br>&lt;br>

        &lt;b>Francois&lt;/b>&lt;br>
        \u25AA French Translation&lt;br>
                &lt;i>https://github.com/franco27&lt;/i>&lt;br>&lt;br>

        &lt;b>gh-pmjm&lt;/b>&lt;br>
        \u25AA Polish translation&lt;br>
                &lt;i>https://github.com/gh-pmjm&lt;/i>&lt;br>&lt;br>

        &lt;b>gr1sh&lt;/b>&lt;br>
        \u25AA fix: some German strings (issues #124, #131)&lt;br>
                &lt;i>https://github.com/gr1sh&lt;/i>&lt;br>&lt;br>

        &lt;b>Harry Heights&lt;/b>&lt;br>
        \u25AA Documentation of FOSS Browser&lt;br>
                &lt;i>https://github.com/HarryHeights&lt;/i>&lt;br>&lt;br>

        &lt;b>Heimen Stoffels&lt;/b>&lt;br>
        \u25AA Dutch translation&lt;br>
                &lt;i>https://github.com/Vistaus&lt;/i>&lt;br>&lt;br>

        &lt;b>Hellohat&lt;/b>&lt;br>
        \u25AA French translation&lt;br>
                &lt;i>https://github.com/Hellohat&lt;/i>&lt;br>&lt;br>

        &lt;b>Herman Nunez&lt;/b>&lt;br>
        \u25AA Spanish translation&lt;br>
                &lt;i>https://github.com/junior012&lt;/i>&lt;br>&lt;br>

        &lt;b>Jumping Yang&lt;/b>&lt;br>
        \u25AA Chinese translation in the previous version of \"FOSS Browser\"&lt;br>
                &lt;i>https://github.com/JumpingYang001&lt;/i>&lt;br>&lt;br>

        &lt;b>lishoujun&lt;/b>&lt;br>
        \u25AA Chinese translation&lt;br>
        \u25AA bug hunting&lt;br>
                &lt;i>https://github.com/lishoujun&lt;/i>&lt;br>&lt;br>

        &lt;b>Peter Bui&lt;/b>&lt;br>
        \u25AA more font sizes to choose&lt;br>
                &lt;i>https://github.com/pbui&lt;/i>&lt;br>&lt;br>

        &lt;b>Secangkir Kopi&lt;/b>&lt;br>
        \u25AA Indonesian translation&lt;br>
                &lt;i>https://github.com/Secangkir-Kopi&lt;/i>&lt;br>&lt;br>

        &lt;b>Sérgio Marques&lt;/b>&lt;br>
        \u25AA Portuguese translation&lt;br>
                &lt;i>https://github.com/smarquespt&lt;/i>&lt;br>&lt;br>

        &lt;b>splinet&lt;/b>&lt;br>
        \u25AA Russian translation in the previous version of \"FOSS Browser\"&lt;br>
                &lt;i>https://github.com/splinet&lt;/i>&lt;br>&lt;br>

        &lt;b>SkewedZeppelin&lt;/b>&lt;br>
        \u25AA Add option to enable Save-Data header&lt;br>
                &lt;i>https://github.com/SkewedZeppelin&lt;/i>&lt;br>&lt;br>

        &lt;b>Tobiplayer3&lt;/b>&lt;br>
        \u25AA added Qwant search engine&lt;br>
        \u25AA option to open new tab instead of exiting app&lt;br>
                &lt;i>https://github.com/Tobiplayer3&lt;/i>&lt;br>&lt;br>

        &lt;b>Vladimir Kosolapov&lt;/b>&lt;br>
        \u25AA Russian translation&lt;br>
                &lt;i>https://github.com/0x264f&lt;/i>&lt;br>&lt;br>

        &lt;b>YC L&lt;/b>&lt;br>
        \u25AA Chinese Translation&lt;br>
                &lt;i>https://github.com/smallg0at&lt;/i>*/


        dialog_text.setMovementMethod(LinkMovementMethod.getInstance());
        dialog.setContentView(dialogView);
        dialog.show();
        HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
    }
}
