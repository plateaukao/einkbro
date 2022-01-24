package de.baumann.browser.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import de.baumann.browser.Ninja.BuildConfig;
import de.baumann.browser.Ninja.R;
import de.baumann.browser.activity.Settings_ClearActivity;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.view.dialog.PrinterDocumentPaperSizeDialog;

public class Fragment_settings extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private boolean showContributors;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_setting, rootKey);

        findPreference("settings_data").setOnPreferenceClickListener(preference -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, new DataSettingsFragment(), "data")
                    .addToBackStack(null)
                    .commit();
            return false;
        });
        findPreference("settings_ui").setOnPreferenceClickListener(preference -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, new UiSettingsFragment(), "ui")
                    .addToBackStack(null)
                    .commit();
            return false;
        });
        findPreference("settings_font").setOnPreferenceClickListener(preference -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, new FontSettingsFragment(), "font")
                    .addToBackStack(null)
                    .commit();
            return false;
        });
        findPreference("settings_pdf_paper_size").setOnPreferenceClickListener(preference -> {
            (new PrinterDocumentPaperSizeDialog(getContext())).show();
            return false;
        });
        findPreference("settings_gesture").setOnPreferenceClickListener(preference -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, new FragmentSettingsGesture(), "gesture")
                    .addToBackStack(null)
                    .commit();
            return false;
        });
        findPreference("settings_start").setOnPreferenceClickListener(preference -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, new Fragment_settings_start(), "start")
                    .addToBackStack(null)
                    .commit();
            return false;
        });
        findPreference("settings_clear").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Settings_ClearActivity.class);
            requireActivity().startActivity(intent);
            return false;
        });
        findPreference("settings_license").setOnPreferenceClickListener(preference -> {
            showContributors = false;
            showLicenseDialog(getString(R.string.license_title), getString(R.string.license_dialog));
            return false;
        });
        findPreference("settings_info").setOnPreferenceClickListener(preference -> {
            showContributors = false;
            showLicenseDialog(getString(R.string.menu_other_info), "v" + BuildConfig.VERSION_NAME + "<br><br>" + getString(R.string.changelog_dialog));
            return false;
        });
        findPreference("settings_appSettings").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", requireActivity().getPackageName(), null);
            intent.setData(uri);
            getActivity().startActivity(intent);
            return false;
        });

        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sp, String key) {
        if (key.equals("userAgent") ||
                key.equals("sp_search_engine_custom") ||
                key.equals("@string/sp_search_engine")
        ) {
            sp.edit().putInt("restart_changed", 1).apply();
        }
    }

    private void showLicenseDialog(String title, String text) {

        final BottomSheetDialog dialog = new BottomSheetDialog(requireActivity());
        View dialogView = View.inflate(getActivity(), R.layout.dialog_text, null);

        dialogView.<TextView>findViewById(R.id.dialog_title).setText(title);

        dialogView.<TextView>findViewById(R.id.dialog_text).setText(HelperUnit.textSpannable(text));

        if (showContributors) {
            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nGaukler Faun\n" +
                    "\u25AA Main developer and initiator of this project\n" +
                    "https://github.com/scoute-dich");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nAli Demirtas\n" +
                    "\u25AA Turkish Translation\n" +
                    "https://github.com/ali-demirtas");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nCGSLURP LLC\n" +
                    "\u25AA Russian translation\n" +
                    "https://crowdin.com/profile/gaich");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nDmitry Gaich\n" +
                    "\u25AA Helped to implement AdBlock and \"request desktop site\" in the previous version of \"EinkBro Browser\".\n" +
                    "https://github.com/futrDevelopment");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nelement54\n" +
                    "\u25AA fix: keyboard problems (issue #105)\n" +
                    "\u25AA new: option to disable confirmation dialogs on exit\n" +
                    "https://github.com/element54");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nelmru\n" +
                    "\u25AA Taiwan Trad. Chinese Translation\n" +
                    "https://github.com/kogiokka");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nEnrico Monese\n" +
                    "\u25AA Italian Translation\n" +
                    "https://github.com/EnricoMonese");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nFrancois\n" +
                    "\u25AA French Translation\n" +
                    "https://github.com/franco27");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\ngh-pmjm\n" +
                    "\u25AA Polish translation\n" +
                    "https://github.com/gh-pmjm");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\ngr1sh\n" +
                    "\u25AA fix: some German strings (issues #124, #131)\n" +
                    "https://github.com/gr1sh");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nHarry Heights\n" +
                    "\u25AA Documentation of EinkBro Browser\n" +
                    " https://github.com/HarryHeights");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nHeimen Stoffels\n" +
                    " \u25AA Dutch translation\n" +
                    "https://github.com/Vistaus");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nHellohat\n" +
                    "\u25AA French translation\n" +
                    "https://github.com/Hellohat");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nHerman Nunez\n" +
                    "\u25AA Spanish translation\n" +
                    "https://github.com/junior012");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nJumping Yang\n" +
                    "\u25AA Chinese translation in the previous version of \"EinkBro Browser\"\n" +
                    "https://github.com/JumpingYang001");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nlishoujun\n" +
                    "\u25AA Chinese translation\n" +
                    "\u25AA bug hunting\n" +
                    "https://github.com/lishoujun");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nLukas Novotny\n" +
                    "\u25AA Czech translation\n" +
                    "https://crowdin.com/profile/novas78");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nOguz Ersen\n" +
                    "\u25AA Turkish translation\n" +
                    "https://crowdin.com/profile/oersen");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nPeter Bui\n" +
                    "\u25AA more font sizes to choose\n" +
                    "https://github.com/pbui");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nRodolfoCandidoB\n" +
                    "\u25AA Portuguese, Brazilian translation\n" +
                    "https://crowdin.com/profile/RodolfoCandidoB");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nSecangkir Kopi\n" +
                    "\u25AA Indonesian translation\n" +
                    "https://github.com/Secangkir-Kopi");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nSérgio Marques\n" +
                    "\u25AA Portuguese translation\n" +
                    "https://github.com/smarquespt");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nsplinet\n" +
                    "\u25AA Russian translation in the previous version of \"EinkBro Browser\"\n" +
                    "https://github.com/splinet");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nSkewedZeppelin\n" +
                    "\u25AA Add option to enable Save-Data header\n" +
                    "https://github.com/SkewedZeppelin");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nTobiplayer\n" +
                    "\u25AA added Qwant search engine\n" +
                    "\u25AA option to open new tab instead of exiting\n" +
                    "https://github.com/Tobiplayer");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nVladimir Kosolapov\n" +
                    "\u25AA Russian translation\n" +
                    "https://github.com/0x264f");

            dialogView.<TextView>findViewById(R.id.dialog_text).append("\n\nYC L\n" +
                    "\u25AA Chinese Translation\n" +
                    "https://github.com/smallg0at");
        }

        dialogView.<TextView>findViewById(R.id.dialog_text).setMovementMethod(LinkMovementMethod.getInstance());
        dialog.setContentView(dialogView);
        dialog.show();
        HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
    }
}
