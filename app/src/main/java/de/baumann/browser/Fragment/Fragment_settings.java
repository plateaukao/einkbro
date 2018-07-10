package de.baumann.browser.Fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.support.design.widget.BottomSheetDialog;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import de.baumann.browser.Activity.Settings_ClearActivity;
import de.baumann.browser.Activity.Settings_DataActivity;
import de.baumann.browser.Activity.Settings_StartActivity;
import de.baumann.browser.Activity.Settings_UIActivity;
import de.baumann.browser.Unit.HelperUnit;
import de.baumann.browser.Ninja.R;
import de.baumann.browser.Unit.IntentUnit;

public class Fragment_settings extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private boolean spChange = false;
    public boolean isSPChange() {
        return spChange;
    }

    private boolean dbChange = false;
    public boolean isDBChange() {
        return dbChange;
    }
    public void setDBChange(boolean dbChange) {
        this.dbChange = dbChange;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_setting);
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
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        switch (preference.getTitleRes()) {

            case R.string.setting_title_data:
                Intent dataControl = new Intent(getActivity(), Settings_DataActivity.class);
                getActivity().startActivityForResult(dataControl, IntentUnit.REQUEST_DATA);
                break;
            case R.string.setting_title_ui:
                Intent uiControl = new Intent(getActivity(), Settings_UIActivity.class);
                getActivity().startActivityForResult(uiControl, IntentUnit.REQUEST_UI);
                break;
            case R.string.setting_title_start_control:
                Intent startControl = new Intent(getActivity(), Settings_StartActivity.class);
                getActivity().startActivityForResult(startControl, IntentUnit.REQUEST_START);
                break;
            case R.string.setting_title_clear_control:
                Intent clearControl = new Intent(getActivity(), Settings_ClearActivity.class);
                getActivity().startActivityForResult(clearControl, IntentUnit.REQUEST_CLEAR);
                break;
            case R.string.setting_title_license:
                showLicenseDialog(getString(R.string.license_title), getString(R.string.license_dialog));
                break;
            case R.string.setting_title_community:
                showLicenseDialog(getString(R.string.setting_title_community), getString(R.string.cont_dialog));
                break;
            case R.string.changelog_title:
                showChangelogDialog();
                break;
            case R.string.setting_title_appSettings:
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                intent.setData(uri);
                getActivity().startActivity(intent);
                break;

            default:
                break;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        spChange = true;
    }

    private void showLicenseDialog(String title, String text) {

        final BottomSheetDialog dialog = new BottomSheetDialog(getActivity());
        View dialogView = View.inflate(getActivity(), R.layout.dialog_text, null);

        TextView dialog_title = dialogView.findViewById(R.id.dialog_title);
        dialog_title.setText(title);

        TextView dialog_text = dialogView.findViewById(R.id.dialog_text);
        dialog_text.setText(HelperUnit.textSpannable(text));
        dialog_text.setMovementMethod(LinkMovementMethod.getInstance());

        ImageButton fab = dialogView.findViewById(R.id.floatButton_ok);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        ImageButton fab_help = dialogView.findViewById(R.id.floatButton_help);
        fab_help.setVisibility(View.GONE);

        ImageButton fab_settings = dialogView.findViewById(R.id.floatButton_settings);
        fab_settings.setVisibility(View.GONE);

        dialog.setContentView(dialogView);
        dialog.show();
    }

    private void showChangelogDialog() {

        final BottomSheetDialog dialog = new BottomSheetDialog(getActivity());
        View dialogView = View.inflate(getActivity(), R.layout.dialog_text, null);

        TextView dialog_title = dialogView.findViewById(R.id.dialog_title);
        dialog_title.setText(R.string.changelog_title);

        TextView dialog_text = dialogView.findViewById(R.id.dialog_text);
        dialog_text.setText(HelperUnit.textSpannable(getString(R.string.changelog_dialog)));
        dialog_text.setMovementMethod(LinkMovementMethod.getInstance());

        ImageButton fab = dialogView.findViewById(R.id.floatButton_ok);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        ImageButton fab_help = dialogView.findViewById(R.id.floatButton_help);
        fab_help.setVisibility(View.GONE);

        ImageButton fab_settings = dialogView.findViewById(R.id.floatButton_settings);
        fab_settings.setVisibility(View.GONE);

        dialog.setContentView(dialogView);
        dialog.show();
    }
}
